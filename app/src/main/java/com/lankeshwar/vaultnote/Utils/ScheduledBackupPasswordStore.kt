package com.lankeshwar.vaultnote.Utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

object ScheduledBackupPasswordStore{

    private const val KEY_ALIAS = "vaultnote_scheduled_backup_password"
    private const val PREFS_NAME = "scheduled_backup_security"
    private const val IV_KEY = "password_iv"
    private const val DATA_KEY = "encrypted_password"

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val existingKey = keyStore.getKey(KEY_ALIAS,null)
        if(existingKey is SecretKey){
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun save(context: Context, password: CharArray) {
        val plainBytes = password.concatToString().toByteArray(Charsets.UTF_8)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getKey())

            val encrypted = cipher.doFinal(plainBytes)

            val preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            preferences.edit {
                putString(
                    IV_KEY,
                    android.util.Base64.encodeToString(
                        cipher.iv,
                        android.util.Base64.NO_WRAP
                    )
                )

                putString(
                    DATA_KEY,
                    android.util.Base64.encodeToString(
                        encrypted,
                        android.util.Base64.NO_WRAP
                    )
                )
            }

        } finally {
            plainBytes.fill(0)
        }
    }

    fun load(context: Context): CharArray?{
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val ivString = preferences.getString(IV_KEY,null) ?: return null

        val encryptedString = preferences.getString(DATA_KEY,null) ?: return null

        val iv = android.util.Base64.decode(ivString, android.util.Base64.NO_WRAP)
        val encrypted = android.util.Base64.decode(encryptedString, android.util.Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,getKey(), GCMParameterSpec(128,iv))

        val decrypted = cipher.doFinal(encrypted)

        return try {
            String(decrypted, Charsets.UTF_8).toCharArray()
        } finally {
            decrypted.fill(0)
        }
    }

    fun clear(context: Context){
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            remove(IV_KEY).remove(DATA_KEY)
        }
    }


}