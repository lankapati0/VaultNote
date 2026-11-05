package com.lankeshwar.vaultnote.data.model.backup

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoManager{
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"

    private const val KEY_SIZE = 256
    private const val GCM_TAG_SIZE = 128
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 600_000

    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 2_000_000

    private const val VERSION = 1

    private val MAGIC = "VNOTEBAK".toByteArray(StandardCharsets.US_ASCII)

    private val secureRandom = SecureRandom()

    /**
     * Encrypts backup data using a password-derived AES-256 key.
     *
     * Returns a portable encrypted backup byte array.
     */
    @Synchronized
    fun encrypt(data: ByteArray,password: CharArray): ByteArray{
        require(password.size >= 8) {
            "Backup password must contain at least 8 characters."
        }

        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)

        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val header = ByteBuffer.allocate(MAGIC.size + 4 + SALT_SIZE + IV_SIZE)
            .order(ByteOrder.BIG_ENDIAN).put(MAGIC).putInt(ITERATIONS)
            .put(salt).put(iv).array()

        val secretKey = deriveKey(password,salt,ITERATIONS)
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE,secretKey, GCMParameterSpec(GCM_TAG_SIZE,iv))

        cipher.updateAAD(header)

        val encryptedData = cipher.doFinal(data)
        return header + encryptedData
    }

    /**
     * Decrypts a backup using its password.
     *
     * Throws BackupAuthenticationException if the password
     * is incorrect or the backup has been modified.
     */
    @Synchronized
    fun decrypt(
        encryptedBackup: ByteArray,
        password: CharArray
    ): ByteArray {

        val headerSize = MAGIC.size + 4 + SALT_SIZE + IV_SIZE

        require(encryptedBackup.size > headerSize + 16) {
            "Invalid or incomplete backup file."
        }

        val buffer = ByteBuffer.wrap(encryptedBackup)
            .order(ByteOrder.BIG_ENDIAN)

        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)

        require(magic.contentEquals(MAGIC)) {
            "This is not a valid VaultNote backup."
        }

        val iterations = buffer.int

        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) {
            "Unsupported backup encryption settings."
        }

        val salt = ByteArray(SALT_SIZE)
        buffer.get(salt)

        val iv = ByteArray(IV_SIZE)
        buffer.get(iv)

        val header = encryptedBackup.copyOfRange(0, headerSize)

        val encryptedData = encryptedBackup.copyOfRange(
            headerSize,
            encryptedBackup.size
        )

        val secretKey = deriveKey(password, salt, iterations)

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(GCM_TAG_SIZE, iv)
            )

            cipher.updateAAD(header)

            cipher.doFinal(encryptedData)

        } catch (exception: AEADBadTagException) {
            throw BackupAuthenticationException(
                "Incorrect password or damaged backup file.",
                exception
            )
        }
    }

    /**
     * Derives a 256-bit AES key from the password and salt.
     */
    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): SecretKeySpec {

        val keySpec = PBEKeySpec(
            password,
            salt,
            iterations,
            KEY_SIZE
        )

        return try {
            val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)

            val keyBytes = factory.generateSecret(keySpec).encoded

            try {
                SecretKeySpec(keyBytes, "AES")
            } finally {
                keyBytes.fill(0)
            }

        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException(
                "Unable to generate backup encryption key.",
                exception
            )
        } finally {
            keySpec.clearPassword()
        }
    }
}

class BackupAuthenticationException(
    message: String,
    cause: Throwable
) : GeneralSecurityException(message, cause)



