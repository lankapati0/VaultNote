package com.lankeshwar.vaultnote.Utils

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {

    private const val PREFS_NAME = "vaultnote_pin_prefs"
    private const val SALT_KEY = "pin_salt"
    private const val HASH_KEY = "pin_hash"

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun hasPin(context: Context): Boolean {
        return preferences(context).contains(HASH_KEY)
    }

    fun savePin(context: Context, pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = hashPin(pin, salt)

        preferences(context).edit()
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = preferences(context)

        val saltString = prefs.getString(SALT_KEY, null) ?: return false
        val hashString = prefs.getString(HASH_KEY, null) ?: return false

        val salt = Base64.decode(saltString, Base64.NO_WRAP)
        val savedHash = Base64.decode(hashString, Base64.NO_WRAP)
        val enteredHash = hashPin(pin, salt)

        return java.security.MessageDigest.isEqual(
            savedHash,
            enteredHash
        )
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}