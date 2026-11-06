package com.lankeshwar.vaultnote.data.model.backup

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExtractedBackup(
    val directory: File,
    val manifestJson: String,
    val extractedFile: Map<String, File>
){
    fun cleanup(){
        directory.deleteRecursively()
    }
}

object BackupArchiveManager{
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val MAX_MANIFEST_SIZE = 16L * 1024 * 1024
    private const val MAX_TOTAL_SIZE = 2L * 1024 * 1024 * 1024

    /**
     * Creates a ZIP archive and encrypts it into the final backup file.
     *
     * files keys are archive entry names.
     * Example: attachments/photo_123.jpg
     */

    fun createEncryptedBackup(
        context: Context,
        manifestJson: String,
        files: Map<String, File>,
        destination: File,
        password: CharArray
    ) {
        android.util.Log.e(
            "VaultNoteBackup",
            "CREATE BACKUP FUNCTION STARTED"
        )
        require(!destination.exists()) {
            "Backup file already exists."
        }

        require(manifestJson.isNotBlank()) {
            "Backup manifest is empty."
        }


        require(files.keys.none { it == MANIFEST_ENTRY }) {
            "Attachment entry conflicts with the backup manifest."
        }

        files.keys.forEach { validateEntryName(it) }

        val temporaryZip = File.createTempFile(
            "vaultnote_archive_",
            ".zip",
            context.cacheDir
        )

        try {
            // Step A: Create the ZIP archive.
            ZipOutputStream(
                BufferedOutputStream(FileOutputStream(temporaryZip))
            ).use { zip ->

                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifestJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                files.forEach { (entryName, sourceFile) ->

                    require(sourceFile.exists() && sourceFile.isFile) {
                        "Backup source file is missing: $entryName"
                    }

                    val originalSize = sourceFile.length()
                    val zipEntry = ZipEntry(entryName).apply {
                        method = ZipEntry.DEFLATED
                    }
                    zip.putNextEntry(zipEntry)


                    sourceFile.inputStream().buffered().use { input ->
                        input.copyTo(zip, 64 * 1024)
                    }

                    zip.closeEntry()
                }
            }

            val zipEntries=  mutableSetOf<String>()

            ZipInputStream(BufferedInputStream(FileInputStream(temporaryZip))).use { testZip ->
                while(true){
                    val entry = testZip.nextEntry ?: break

                    testZip.closeEntry()
                }
            }



            // Step B: Verify the ZIP file.
            check(temporaryZip.exists() && temporaryZip.length() > 0L) {
                "ZIP creation failed. ZIP size: ${temporaryZip.length()} bytes."
            }

            val zipSize = temporaryZip.length()

            java.util.zip.ZipFile(temporaryZip).use {zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()){
                    val entry = entries.nextElement()

                    android.util.Log.d("VaultNoteBackup", "ZIP RESULT: ${entry.name} | " + "Original: ${entry.size} bytes | " + "Compressed: ${entry.compressedSize} bytes")
                }
            }
            android.util.Log.d("VaultNoteBackup", "TOTAL ZIP SIZE: ${zipSize / (1024 * 1024)} MB")

            // Step C: Encrypt the ZIP file.
            FileInputStream(temporaryZip).use { input ->
                StreamingBackupCryptoManager.encrypt(
                    input = input,
                    destination = destination,
                    password = password
                )
            }

            // Step D: Verify the final encrypted backup.
            check(destination.exists() && destination.length() > 0L) {
                "Encryption completed, but backup file is empty."
            }

            check(destination.length() > 0L) {
                "Final backup size is zero."
            }

        } finally {
            temporaryZip.delete()
        }
    }


    /**
     * Decrypts a backup and safely extracts its ZIP contents
     * into a temporary app-private directory.
     */
    fun extractEncryptedBackup(
        context: Context,
        backupFile: File,
        password: CharArray
    ): ExtractedBackup {

        require(backupFile.exists() && backupFile.isFile) {
            "Backup file does not exist."
        }

        val stagingDirectory = File(
            context.cacheDir,
            "vaultnote_restore_${UUID.randomUUID()}"
        )

        check(stagingDirectory.mkdirs()) {
            "Unable to create restore directory."
        }

        val decryptedZip = File(stagingDirectory, "decrypted.zip")
        val extractedDirectory = File(stagingDirectory, "extracted")

        try {
            check(extractedDirectory.mkdirs()) {
                "Unable to create extraction directory."
            }

            StreamingBackupCryptoManager.decrypt(
                source = backupFile,
                destination = decryptedZip,
                password = password
            )

            var manifestJson: String? = null
            var totalExtractedBytes = 0L

            val extractedFiles = mutableMapOf<String, File>()
            val seenEntries = mutableSetOf<String>()

            ZipInputStream(
                BufferedInputStream(FileInputStream(decryptedZip))
            ).use { zip ->

                while (true) {
                    val entry = zip.nextEntry ?: break

                    val entryName = entry.name

                    validateEntryName(entryName)

                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }

                    val outputFile = File(
                        extractedDirectory,
                        entryName
                    )

                    val canonicalRoot =
                        extractedDirectory.canonicalFile.path +
                                File.separator

                    val canonicalOutput = outputFile.canonicalFile

                    require(
                        canonicalOutput.path.startsWith(canonicalRoot)
                    ) {
                        "Invalid backup file path."
                    }

                    check(
                        seenEntries.add(entryName)
                    ) {
                        android.util.Log.e(
                            "VaultNoteBackup",
                            "DUPLICATE ZIP ENTRY: $entryName"
                        )
                        "Duplicate backup entry detected: $entryName"
                    }

                    outputFile.parentFile?.let { parent ->
                        check(parent.exists() || parent.mkdirs()) {
                            "Unable to create extraction directory."
                        }
                    }

                    if (entryName == MANIFEST_ENTRY) {

                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var entrySize = 0L

                        while (true) {
                            val count = zip.read(buffer)

                            if (count == -1) break

                            entrySize += count

                            require(entrySize <= MAX_MANIFEST_SIZE) {
                                "Backup manifest is too large."
                            }

                            output.write(buffer, 0, count)
                        }

                        totalExtractedBytes += entrySize

                        manifestJson = output.toString("UTF-8")

                    } else {

                        FileOutputStream(outputFile).use { output ->

                            val buffer = ByteArray(64 * 1024)

                            while (true) {
                                val count = zip.read(buffer)

                                if (count == -1) break

                                totalExtractedBytes += count

                                require(
                                    totalExtractedBytes <= MAX_TOTAL_SIZE
                                ) {
                                    "Backup extraction exceeds the allowed size."
                                }

                                output.write(buffer, 0, count)
                            }
                        }

                        extractedFiles[entryName] = outputFile
                    }

                    zip.closeEntry()
                }
            }

            require(!manifestJson.isNullOrBlank()) {
                "Backup manifest is missing."
            }

            decryptedZip.delete()

            return ExtractedBackup(
                directory = stagingDirectory,
                manifestJson = manifestJson,
                extractedFile = extractedFiles
            )

        } catch (exception: Exception) {
            stagingDirectory.deleteRecursively()
            throw exception
        }
    }

    /**
     * Rejects unsafe ZIP entry names and path traversal.
     */
    private fun validateEntryName(name: String) {

        require(name.isNotBlank()) {
            "Invalid backup entry name."
        }

        require(!name.startsWith("/") && !name.startsWith("\\")) {
            "Absolute backup paths are not allowed."
        }

        require(!name.contains("\\")) {
            "Invalid backup entry path."
        }

        val segments = name.split("/")

        require(
            segments.none {
                it.isBlank() || it == "." || it == ".."
            }
        ) {
            "Unsafe backup entry path."
        }

        require(!name.contains(":")) {
            "Invalid backup entry name."
        }
    }
}