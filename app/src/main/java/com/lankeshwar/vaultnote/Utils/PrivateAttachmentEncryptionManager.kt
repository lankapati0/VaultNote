package com.lankeshwar.vaultnote.Utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.io.DataInputStream
object PrivateAttachmentEncryptionManager {
    private const val KEY_ALIAS = "vaultnote_private_attachments_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    private const val MAGIC = "VA1:"
    @Synchronized
    fun encryptFile(source: File, destination: File) {
        require(source.exists()) {
            "Source attachment does not exist."
        }

        require(!destination.exists()){
            "Destination attachment already exists."
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val iv = cipher.iv
        val parent =destination.absoluteFile.parentFile ?: throw IllegalStateException("Invalid destination directory.")
        parent.mkdirs()
        val temporaryFile = File.createTempFile("vn_encrypt",".tmp",parent)
        try {
            source.inputStream().buffered().use { input ->
                temporaryFile.outputStream().buffered().use { output ->

                    output.write(MAGIC.toByteArray(Charsets.UTF_8))
                    output.write(iv)

                    val buffer = ByteArray(64 * 1024)

                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break

                        val encrypted = cipher.update(buffer, 0, count)
                        if (encrypted != null) {
                            output.write(encrypted)
                        }
                    }

                    val finalBytes = cipher.doFinal()
                    output.write(finalBytes)
                }
            }

            check(temporaryFile.renameTo(destination)) {
                "Unable to save encrypted attachment."
            }

        } catch (e: Exception) {
            temporaryFile.delete()
            throw e
        }
    }

    @Synchronized
    fun decryptFile(source: File, destination: File) {
        require(source.exists()) {
            "Encrypted attachment does not exist."
        }

        require(!destination.exists()) {
            "Destination file already exists."
        }

        val magicBytes = MAGIC.toByteArray(Charsets.UTF_8)

        val tagSizeBytes = TAG_SIZE / 8

        val headerSize = magicBytes.size + IV_SIZE

        require(source.length() > headerSize + tagSizeBytes) {
            "Invalid encrypted attachment."
        }

        val parent = destination.absoluteFile.parentFile
            ?: throw IllegalStateException("Invalid destination directory.")

        parent.mkdirs()

        val temporaryFile = File.createTempFile(
            "vn_decrypt_",
            ".tmp",
            parent
        )

        try {
            DataInputStream(source.inputStream().buffered()).use { input ->

                val header = ByteArray(magicBytes.size)
                input.readFully(header)

                require(header.contentEquals(magicBytes)) {
                    "Attachment is not encrypted."
                }

                val iv = ByteArray(IV_SIZE)
                input.readFully(iv)

                val cipher = Cipher.getInstance(TRANSFORMATION)

                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getExistingSecretKey(),
                    GCMParameterSpec(TAG_SIZE, iv)
                )

                val encryptedContentSize =
                    source.length() - headerSize - tagSizeBytes

                temporaryFile.outputStream().buffered().use { output ->

                    val buffer = ByteArray(64 * 1024)
                    var remaining = encryptedContentSize

                    while (remaining > 0) {
                        val count = input.read(
                            buffer,
                            0,
                            minOf(buffer.size.toLong(), remaining).toInt()
                        )

                        if (count == -1) {
                            throw IllegalStateException(
                                "Encrypted attachment is incomplete."
                            )
                        }

                        val decrypted = cipher.update(buffer, 0, count)

                        if (decrypted != null) {
                            output.write(decrypted)
                        }

                        remaining -= count
                    }

                    // Read the authentication tag separately.
                    val tag = ByteArray(tagSizeBytes)
                    input.readFully(tag)

                    // Authentication happens here.
                    // If verification fails, decryption throws an exception.
                    val finalBytes = cipher.doFinal(tag)

                    output.write(finalBytes)
                }
            }

            check(temporaryFile.renameTo(destination)) {
                "Unable to save decrypted attachment."
            }

        } catch (e: Exception) {
            temporaryFile.delete()
            throw e
        }
    }

    fun isEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() < MAGIC.length) {
            return false
        }

        return file.inputStream().use { input ->
            val header = ByteArray(MAGIC.length)
            input.read(header) == header.size &&
                    header.contentEquals(
                        MAGIC.toByteArray(Charsets.UTF_8)
                    )
        }
    }

    @Synchronized
    private fun getExistingSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val existingKey = keyStore.getKey(KEY_ALIAS, null)

        return existingKey as? SecretKey
            ?: throw IllegalStateException(
                "Private attachment encryption key is missing."
            )
    }

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