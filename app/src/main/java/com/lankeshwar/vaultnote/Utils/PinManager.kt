package com.lankeshwar.vaultnote.Utils

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.core.content.edit
import java.security.MessageDigest

object PinManager {

    private const val PREFS_NAME = "vaultnote_pin_prefs"
    private const val SALT_KEY = "pin_salt"
    private const val HASH_KEY = "pin_hash"

    private const val LEGACY_ITERATIONS = 120_000
    private const val CURRENT_ITERATIONS = 600_000
    private const val KEY_LENGTH = 256
    private const val REAL_ITERATIONS_KEY = "pin_hash_iterations"
    private const val DECOY_ITERATIONS_KEY = "decoy_pin_hash_iterations"
    private const val FAILED_ATTEMPT_KEY = "failed_pin_attempts"
    private const val LOCK_UNTIL_KEY = "pin_lock_until"
    private const val MAX_ATTEMPTS = 5
    private const val LOCK_DURATION_MS = 30_000L
    private const val DECOY_SALT_KEY = "decoy_pin_salt"
    private const val DECOY_HASH_KEY = "decoy_pin_hash"

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun hasPin(context: Context): Boolean{
        return preferences(context).contains(HASH_KEY)
    }


    fun hasDecoyPin(context: Context): Boolean {
        return preferences(context).contains(DECOY_HASH_KEY)
    }

    fun remainingLockTime(context: Context):Long {
        val prefs = preferences(context)
        val lockUntil = prefs.getLong(LOCK_UNTIL_KEY,0L)
        val remaining = lockUntil - System.currentTimeMillis()

        return if (remaining > 0L) remaining else 0L
    }

    fun recordFailedAttempt(context: Context): Int {
        val prefs = preferences(context)
        val attempts = prefs.getInt(FAILED_ATTEMPT_KEY,0)+1
        prefs.edit { putInt(FAILED_ATTEMPT_KEY, attempts) }

        if(attempts >= MAX_ATTEMPTS){
            prefs.edit{
                putLong(LOCK_UNTIL_KEY, System.currentTimeMillis() + LOCK_DURATION_MS)
                putInt(FAILED_ATTEMPT_KEY,0)
            }
            return MAX_ATTEMPTS
        }
        prefs.edit{
            putInt(FAILED_ATTEMPT_KEY,attempts)
        }
        return attempts
    }

    fun resetFailedAttempts(context: Context){
        preferences(context).edit { remove(FAILED_ATTEMPT_KEY).remove(LOCK_UNTIL_KEY) }
    }

    fun savePin(context: Context, pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = hashPin(pin, salt,CURRENT_ITERATIONS)

        preferences(context).edit{
            putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            putInt(REAL_ITERATIONS_KEY,CURRENT_ITERATIONS)
            apply()
        }
    }

    fun saveDecoyPin(context: Context,pin: String){
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = hashPin(pin,salt,CURRENT_ITERATIONS)
        preferences(context).edit{
            putString(DECOY_SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(DECOY_HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            putInt(DECOY_ITERATIONS_KEY,CURRENT_ITERATIONS)
            apply()
        }
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = preferences(context)

        val saltString = prefs.getString(SALT_KEY, null) ?: return false
        val hashString = prefs.getString(HASH_KEY, null) ?: return false

        return try {
            val salt = Base64.decode(saltString, Base64.NO_WRAP)
            val savedHash = Base64.decode(hashString, Base64.NO_WRAP)
            val iteration= prefs.getInt(REAL_ITERATIONS_KEY,LEGACY_ITERATIONS)
            val enteredHash = hashPin(pin, salt,iteration)

            if(!MessageDigest.isEqual(savedHash,enteredHash)){
                return false
            }

            // Upgrade an older hash after successful verification.
            if(iteration < CURRENT_ITERATIONS){
                val newSalt = ByteArray(16)
                SecureRandom().nextBytes(newSalt)
                val newHash = hashPin(pin,newSalt,CURRENT_ITERATIONS)

                prefs.edit {
                    putString(SALT_KEY, Base64.encodeToString(newSalt, Base64.NO_WRAP))
                    putString(HASH_KEY,Base64.encodeToString(newHash, Base64.NO_WRAP))
                    putInt(REAL_ITERATIONS_KEY,CURRENT_ITERATIONS)
                    apply()
                }
            }
            true
        }catch (e: IllegalArgumentException){
            false
        }
    }

    fun isSameAsRealPin(context: Context,pin: String): Boolean{
        return verifyPin(context,pin)
    }

    fun verifyDecoyPin(context: Context,pin: String): Boolean{
        val prefs = preferences(context)

        val saltString = prefs.getString(DECOY_SALT_KEY, null)
            ?: return false

        val hashString = prefs.getString(DECOY_HASH_KEY, null)
            ?: return false

        return try {
            val salt = Base64.decode(saltString, Base64.NO_WRAP)
            val savedHash = Base64.decode(hashString, Base64.NO_WRAP)
            val iterations = prefs.getInt(DECOY_ITERATIONS_KEY,LEGACY_ITERATIONS)
            val enteredHash = hashPin(pin,salt,iterations)
            if(!MessageDigest.isEqual(savedHash,enteredHash)){return false}

            if(iterations<CURRENT_ITERATIONS){
                val newSalt = ByteArray(16)
                SecureRandom().nextBytes(newSalt)

                val newHash = hashPin(pin,newSalt,CURRENT_ITERATIONS)
                prefs.edit {
                    putString(DECOY_SALT_KEY, Base64.encodeToString(newSalt, Base64.NO_WRAP))
                    putString(DECOY_HASH_KEY, Base64.encodeToString(newHash, Base64.NO_WRAP))
                    putInt(DECOY_ITERATIONS_KEY, CURRENT_ITERATIONS)
                    apply()
                }
            }
            true
        }catch (e: IllegalArgumentException){
            false
        }

    }

    private fun hashPin(pin: String, salt: ByteArray,iterations:Int): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            iterations,
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