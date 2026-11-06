package com.lankeshwar.vaultnote.data.model.backup

import java.util.zip.ZipInputStream
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
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object StreamingBackupCryptoManager {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"

    private const val KEY_SIZE = 256
    private const val TAG_SIZE = 128
    private const val TAG_BYTES = TAG_SIZE / 8

    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12

    private const val ITERATIONS = 600_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 2_000_000

    // Version 1: previous single-stream AES-GCM format.
    // Version 2: independently authenticated chunks.
    private const val VERSION = 2
    private const val LEGACY_VERSION = 1

    private const val BUFFER_SIZE = 64 * 1024

    private val MAGIC =
        "VNOTEBAK".toByteArray(StandardCharsets.US_ASCII)

    private val secureRandom = SecureRandom()

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
        val baseIv = ByteArray(IV_SIZE)

        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(baseIv)

        val header = ByteBuffer.allocate(
            MAGIC.size + 4 + 4 + SALT_SIZE + IV_SIZE
        ).order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC)
            .putInt(VERSION)
            .putInt(ITERATIONS)
            .put(salt)
            .put(baseIv)
            .array()

        val key = deriveKey(password, salt, ITERATIONS)

        val temporaryFile = File.createTempFile(
            "vaultnote_backup_",
            ".tmp",
            parent
        )

        try {
            var inputBytes = 0L
            var encryptedBytes = 0L
            var chunkIndex = 0L

            BufferedOutputStream(
                FileOutputStream(temporaryFile)
            ).use { output ->

                output.write(header)
                encryptedBytes += header.size

                val buffer = ByteArray(BUFFER_SIZE)

                while (true) {
                    val count = readChunk(input, buffer)

                    if (count == -1) break

                    inputBytes += count

                    val cipher = Cipher.getInstance(TRANSFORMATION)

                    cipher.init(
                        Cipher.ENCRYPT_MODE,
                        key,
                        GCMParameterSpec(
                            TAG_SIZE,
                            createNonce(baseIv, chunkIndex)
                        )
                    )

                    cipher.updateAAD(
                        createAad(header, chunkIndex, count)
                    )

                    // Each chunk is encrypted independently.
                    val encrypted = cipher.doFinal(buffer, 0, count)

                    output.writeIntValue(count)
                    output.write(encrypted)

                    encryptedBytes += 4 + encrypted.size

                    check(chunkIndex < Long.MAX_VALUE) {
                        "Backup contains too many chunks."
                    }

                    chunkIndex++
                }

                check(inputBytes > 0L) {
                    "ZIP input is empty."
                }

                // Authenticated end marker.
                // This allows restore to detect truncated backups.
                val finalCipher = Cipher.getInstance(TRANSFORMATION)

                finalCipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    GCMParameterSpec(
                        TAG_SIZE,
                        createNonce(baseIv, chunkIndex)
                    )
                )

                finalCipher.updateAAD(
                    createAad(header, chunkIndex, 0)
                )

                val finalTag = finalCipher.doFinal()

                output.writeIntValue(0)
                output.write(finalTag)

                encryptedBytes += 4 + finalTag.size

                output.flush()
            }

            check(temporaryFile.length() == encryptedBytes) {
                "Encryption write mismatch."
            }

            check(temporaryFile.renameTo(destination)) {
                "Unable to save encrypted backup."
            }

            check(destination.exists() && destination.length() > 0L) {
                "Encrypted destination is empty after rename."
            }

        } catch (exception: Throwable) {
            temporaryFile.delete()
            destination.delete()
            throw exception
        }
    }

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

        require(source.length() > headerSize + TAG_BYTES) {
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
            BufferedInputStream(
                FileInputStream(source)
            ).use { rawInput ->

                val input = DataInputStream(rawInput)

                val magic = ByteArray(MAGIC.size)
                input.readFully(magic)

                require(magic.contentEquals(MAGIC)) {
                    "This is not a VaultNote backup."
                }

                val version = input.readInt()

                require(
                    version == VERSION ||
                            version == LEGACY_VERSION
                ) {
                    "Unsupported backup format version."
                }

                val iterations = input.readInt()

                require(
                    iterations in MIN_ITERATIONS..MAX_ITERATIONS
                ) {
                    "Unsupported backup encryption settings."
                }

                val salt = ByteArray(SALT_SIZE)
                input.readFully(salt)

                val baseIv = ByteArray(IV_SIZE)
                input.readFully(baseIv)

                val header = ByteBuffer.allocate(headerSize)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(MAGIC)
                    .putInt(version)
                    .putInt(iterations)
                    .put(salt)
                    .put(baseIv)
                    .array()

                val key = deriveKey(password, salt, iterations)

                BufferedOutputStream(
                    FileOutputStream(temporaryFile)
                ).use { output ->

                    when (version) {
                        LEGACY_VERSION -> {
                            // Keep compatibility with existing backups.
                            decryptLegacy(
                                input = input,
                                output = output,
                                header = header,
                                baseIv = baseIv,
                                key = key
                            )
                        }

                        VERSION -> {
                            decryptChunked(
                                input = input,
                                output = output,
                                header = header,
                                baseIv = baseIv,
                                key = key
                            )
                        }
                    }

                    output.flush()
                }
            }

            verifyZipEntries(temporaryFile)

            check(temporaryFile.renameTo(destination)) {
                "Unable to save restored backup data."
            }

        } catch (exception: AEADBadTagException) {
            temporaryFile.delete()

            throw BackupAuthenticationException(
                "Incorrect password or damaged backup file.",
                exception
            )

        } catch (exception: Throwable) {
            temporaryFile.delete()
            throw exception
        }
    }

    private fun decryptChunked(
        input: DataInputStream,
        output: BufferedOutputStream,
        header: ByteArray,
        baseIv: ByteArray,
        key: SecretKeySpec
    ) {
        var chunkIndex = 0L
        var totalBytes = 0L

        while (true) {
            val plainLength = input.readInt()

            // Zero means the authenticated end marker.
            if (plainLength == 0) {

                val cipher = Cipher.getInstance(TRANSFORMATION)

                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(
                        TAG_SIZE,
                        createNonce(baseIv, chunkIndex)
                    )
                )

                cipher.updateAAD(
                    createAad(header, chunkIndex, 0)
                )

                val finalTag = ByteArray(TAG_BYTES)
                input.readFully(finalTag)

                // Verifies the final authentication tag.
                cipher.doFinal(finalTag)

                check(input.read() == -1) {
                    "Unexpected data after backup end marker."
                }

                check(totalBytes > 0L) {
                    "Backup contains no ZIP data."
                }

                break
            }

            require(plainLength in 1..BUFFER_SIZE) {
                "Invalid backup chunk size."
            }

            val encrypted = ByteArray(plainLength + TAG_BYTES)
            input.readFully(encrypted)

            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(
                    TAG_SIZE,
                    createNonce(baseIv, chunkIndex)
                )
            )

            cipher.updateAAD(
                createAad(header, chunkIndex, plainLength)
            )

            // Each chunk is authenticated before being written.
            val decrypted = cipher.doFinal(encrypted)

            check(decrypted.size == plainLength) {
                "Invalid decrypted chunk size."
            }

            output.write(decrypted)
            totalBytes += decrypted.size

            check(chunkIndex < Long.MAX_VALUE) {
                "Backup contains too many chunks."
            }

            chunkIndex++
        }
    }

    private fun decryptLegacy(
        input: DataInputStream,
        output: BufferedOutputStream,
        header: ByteArray,
        baseIv: ByteArray,
        key: SecretKeySpec
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_SIZE, baseIv)
        )

        cipher.updateAAD(header)

        val buffer = ByteArray(BUFFER_SIZE)

        while (true) {
            val count = input.read(buffer)

            if (count == -1) break
            if (count == 0) continue

            val decrypted = cipher.update(buffer, 0, count)

            if (decrypted != null) {
                output.write(decrypted)
            }
        }

        // Legacy version 1 authentication.
        output.write(cipher.doFinal())
    }

    private fun createNonce(
        baseIv: ByteArray,
        chunkIndex: Long
    ): ByteArray {
        return ByteBuffer.allocate(IV_SIZE)
            .order(ByteOrder.BIG_ENDIAN)
            .put(baseIv, 0, 4)
            .putLong(chunkIndex)
            .array()
    }

    private fun createAad(
        header: ByteArray,
        chunkIndex: Long,
        plainLength: Int
    ): ByteArray {
        return ByteBuffer.allocate(header.size + 8 + 4)
            .order(ByteOrder.BIG_ENDIAN)
            .put(header)
            .putLong(chunkIndex)
            .putInt(plainLength)
            .array()
    }

    private fun readChunk(
        input: InputStream,
        buffer: ByteArray
    ): Int {
        var total = 0

        while (total < buffer.size) {
            val count = input.read(
                buffer,
                total,
                buffer.size - total
            )

            if (count == -1) {
                return if (total == 0) -1 else total
            }

            if (count == 0) {
                val singleByte = input.read()

                if (singleByte == -1) {
                    return if (total == 0) -1 else total
                }

                buffer[total++] = singleByte.toByte()
            } else {
                total += count
            }
        }

        return total
    }

    private fun verifyZipEntries(file: File) {
        val entries = mutableSetOf<String>()

        java.util.zip.ZipInputStream(
            BufferedInputStream(FileInputStream(file))
        ).use { zip ->

            while (true) {
                val entry = zip.nextEntry ?: break

                check(entries.add(entry.name)) {
                    "Duplicate entry in decrypted backup."
                }

                zip.closeEntry()
            }
        }

        check(entries.isNotEmpty()) {
            "Decrypted backup contains no ZIP entries."
        }
    }

    private fun BufferedOutputStream.writeIntValue(value: Int) {
        write(
            ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(value)
                .array()
        )
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
