package com.lankeshwar.vaultnote.data.model.backup

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.ZipInputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object StreamingBackupCryptoManager{
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val KEY_SIZE = 256
    private const val TAG_SIZE = 128
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 600_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 2_000_000
    private const val VERSION = 1
    private val MAGIC = "VNOTEBAK".toByteArray(StandardCharsets.US_ASCII)
    private val secureRandom = SecureRandom()
    /**
     * Encrypts an input stream directly into a backup file.
     */
    @Synchronized
    fun encrypt(
        input: InputStream,
        destination: File,
        password: CharArray
    ) {
        require(password.size >= 6) {
            "Backup password must contain at least 6 characters."
        }

        require(!destination.exists()) {
            "Backup destination already exists."
        }

        val parent = destination.absoluteFile.parentFile
            ?: throw IllegalStateException("Invalid backup directory.")

        check(parent.exists() || parent.mkdirs()) {
            "Unable to create backup directory."
        }

        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)

        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val header = ByteBuffer.allocate(
            MAGIC.size + 4 + 4 + SALT_SIZE + IV_SIZE
        ).order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC)
            .putInt(VERSION)
            .putInt(ITERATIONS)
            .put(salt)
            .put(iv)
            .array()

        val key = deriveKey(password, salt, ITERATIONS)

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))

        cipher.updateAAD(header)

        val temporaryFile = File.createTempFile(
            "vaultnote_backup_",
            ".tmp",
            parent
        )

        var inputBytes = 0L
        var encryptedBytes = 0L

        try {
            BufferedOutputStream(
                FileOutputStream(temporaryFile)
            ).use { output ->

                output.write(header)
                encryptedBytes += header.size

                val buffer = ByteArray(64 * 1024)

                while (true) {
                    val count = input.read(buffer)

                    if (count == -1) break
                    if(count == 0) continue
                    inputBytes += count

                    val encrypted = cipher.update(buffer, 0, count)

                    if (encrypted != null) {
                        output.write(encrypted)
                        encryptedBytes += encrypted.size
                    }
                }
                val finalEncrypted = cipher.doFinal()
                output.write(finalEncrypted)
                encryptedBytes += finalEncrypted.size
                output.flush()
            }

            check(inputBytes > 0L) {
                "ZIP input is empty. Input bytes: $inputBytes"
            }

            check(temporaryFile.length() == encryptedBytes) {
                "Encryption write mismatch. Expected: $encryptedBytes, actual: ${temporaryFile.length()}"
            }

            check(temporaryFile.length() > 0L) {
                "Encrypted temporary file is empty."
            }

            check(temporaryFile.renameTo(destination)) {
                "Unable to save encrypted backup."
            }

            check(destination.exists() && destination.length() > 0L) {
                "Encrypted destination is empty after rename."
            }

        } catch (exception: Exception) {
            temporaryFile.delete()
            destination.delete()
            throw exception
        }
    }

    /**
     * Decrypts a backup into a temporary file.
     * The destination is created only after authentication succeeds.
     */
    @Synchronized
    fun decrypt(
        source: File,
        destination: File,
        password: CharArray
    ) {
        require(source.exists()) {
            "Backup file does not exist."
        }

        require(!destination.exists()) {
            "Restore destination already exists."
        }

        val headerSize =
            MAGIC.size + 4 + 4 + SALT_SIZE + IV_SIZE

        require(source.length() > headerSize + 16) {
            "Invalid or incomplete backup file."
        }

        val parent = destination.absoluteFile.parentFile
            ?: throw IllegalStateException("Invalid restore directory.")

        check(parent.exists() || parent.mkdirs()) {
            "Unable to create restore directory."
        }

        val temporaryFile = File.createTempFile(
            "vaultnote_restore_",
            ".tmp",
            parent
        )

        try {
            BufferedInputStream(FileInputStream(source)).use { rawInput ->

                val input = DataInputStream(rawInput)

                val magic = ByteArray(MAGIC.size)
                input.readFully(magic)

                require(magic.contentEquals(MAGIC)) {
                    "This is not a VaultNote backup."
                }

                val version = input.readInt()

                require(version == VERSION) {
                    "Unsupported backup format version."
                }

                val iterations = input.readInt()

                require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) {
                    "Unsupported backup encryption settings."
                }

                val salt = ByteArray(SALT_SIZE)
                input.readFully(salt)

                val iv = ByteArray(IV_SIZE)
                input.readFully(iv)

                val header = ByteBuffer.allocate(headerSize)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(MAGIC)
                    .putInt(version)
                    .putInt(iterations)
                    .put(salt)
                    .put(iv)
                    .array()

                val key = deriveKey(password, salt, iterations)

                val cipher = Cipher.getInstance(TRANSFORMATION)

                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(TAG_SIZE, iv)
                )

                cipher.updateAAD(header)

                BufferedOutputStream(
                    FileOutputStream(temporaryFile)
                ).use { output ->

                    val buffer = ByteArray(64 * 1024)

                    while (true) {
                        val count = input.read(buffer)

                        if (count == -1) break

                        val decrypted = cipher.update(buffer, 0, count)

                        if (decrypted != null) {
                            output.write(decrypted)
                        }
                    }

                    // Authentication is verified here.
                    output.write(cipher.doFinal())
                    output.flush()
               }
            }

            val decryptedEntries = mutableSetOf<String>()
            ZipInputStream(BufferedInputStream(FileInputStream(temporaryFile))).use { testZip ->
                while(true){
                    val entry = testZip.nextEntry ?: break
                    android.util.Log.d("VaultNoteBackup", "DECRYPTED TEMP ENTRY: [${entry.name}]")

                    if(!decryptedEntries.add(entry.name)){
                        android.util.Log.d("VaultNoteBackup", "DUPLICATE IN DECRYPTED TEMP ZIP: [${entry.name}]")
                    }
                    testZip.closeEntry()
                }


            }

            check(temporaryFile.renameTo(destination)) {
                "Unable to save restored backup data."
            }

        } catch (exception: AEADBadTagException) {
            temporaryFile.delete()

            throw BackupAuthenticationException(
                "Incorrect password or damaged backup file.",
                exception
            )

        } catch (exception: Exception) {
            temporaryFile.delete()
            throw exception
        }
    }

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