package com.lankeshwar.vaultnote.Utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object PrivateNoteEncryptionManager {
    private const val KEY_ALIAS = "vaultnote_private_notes_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    private const val ENCRYPTED_PREFIX = "VN1:"

    @Synchronized
    fun encrypt(plainText:String):String{
        val secretKey = getOrCreateSecretKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv

        val encryptedBytes = cipher.doFinal(
            plainText.toByteArray(StandardCharsets.UTF_8)
        )

        val encodedIv = android.util.Base64.encodeToString(
            iv,
            android.util.Base64.NO_WRAP
        )
        val encodedCipherText = android.util.Base64.encodeToString(
            encryptedBytes,
            android.util.Base64.NO_WRAP
        )

        return "$ENCRYPTED_PREFIX$encodedIv:$encodedCipherText"
    }

    @Synchronized
    fun decrypt(encryptedText: String): String {

        require(isEncrypted(encryptedText)) {
            "This value is not encrypted private-note data."
        }

        val parts = encryptedText.split(":")

        require(parts.size == 3 && parts[0] == "VN1") {
            "Invalid encrypted data format."
        }

        val iv = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
        val encryptedBytes = android.util.Base64.decode(parts[2], android.util.Base64.NO_WRAP)

        require(iv.size == IV_SIZE) {
            "Invalid encryption IV."
        }

        val secretKey = getExistingSecretKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(TAG_SIZE, iv)
        )

        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(
            decryptedBytes,
            StandardCharsets.UTF_8
        )
    }

    /**
     * Checks whether a value uses our encrypted-data format.
     */
    fun isEncrypted(value: String): Boolean {
        return value.startsWith(ENCRYPTED_PREFIX)
    }

    @Synchronized
    private fun getExistingSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val existingKey = keyStore.getKey(KEY_ALIAS,null)
        return existingKey as? SecretKey
            ?: throw IllegalStateException(
                "Private-note encryption key is missing. Encrypted notes cannot be decrypted."
            )
    }

    /**
     * Retrieves the existing AES key or creates it securely
     * inside Android Keystore.
     */
    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val existingKey = keyStore.getKey(KEY_ALIAS, null)

        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

}