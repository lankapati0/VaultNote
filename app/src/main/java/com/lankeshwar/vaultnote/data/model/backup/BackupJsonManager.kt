package com.lankeshwar.vaultnote.data.model.backup

import kotlinx.serialization.json.Json
import java.io.File


object BackupJsonManager {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    /**
     * Converts VaultNote backup data into JSON.
     */
    fun encodeBackup(backup: VaultNoteBackup): String {
        return json.encodeToString(backup)
    }

    /**
     * Converts backup JSON back into VaultNote backup data.
     */
    fun decodeBackup(jsonData: String): VaultNoteBackup {

        val backup = json.decodeFromString<VaultNoteBackup>(jsonData)

        require(backup.appName == "VaultNote") {
            "This backup does not belong to VaultNote."
        }

        require(backup.formatVersion == 1) {
            "Unsupported VaultNote backup version."
        }

        validateBackupStructure(backup)

        return backup
    }

    /**
     * Validates IDs and relationships between backup records.
     */
    private fun validateBackupStructure(backup: VaultNoteBackup) {

        require(backup.createdAt > 0) {
            "Invalid backup creation date."
        }

        // Unique IDs
        requireUnique(backup.notes.map { it.id }, "note")
        requireUnique(backup.attachments.map { it.id }, "attachment")
        requireUnique(backup.tags.map { it.id }, "tag")
        requireUnique(backup.drafts.map { it.draftId }, "draft")
        requireUnique(backup.tasks.map { it.id }, "task")
        requireUnique(backup.journalEntries.map { it.id }, "journal entry")
        requireUnique(backup.journalPhotos.map { it.id }, "journal photo")

        require(backup.drafts.all { it.draftId.isNotBlank() }) {
            "Backup contains an invalid draft ID."
        }

        // Validate note attachment relationships
        val noteIds = backup.notes.map { it.id }.toSet()

        backup.attachments.forEach { attachment ->
            require(attachment.noteId in noteIds) {
                "An attachment references a note that does not exist."
            }
        }

        // Validate tag relationships
        val tagIds = backup.tags.map { it.id }.toSet()

        val crossRefPairs = mutableSetOf<Pair<Int, Int>>()

        backup.noteTagCrossRefs.forEach { crossRef ->

            require(crossRef.noteId in noteIds) {
                "A tag relationship references a missing note."
            }

            require(crossRef.tagId in tagIds) {
                "A tag relationship references a missing tag."
            }

            require(
                crossRefPairs.add(
                    crossRef.noteId to crossRef.tagId
                )
            ) {
                "Duplicate note-tag relationship detected."
            }
        }

        // Validate journal photo relationships
        val journalIds = backup.journalEntries.map { it.id }.toSet()

        backup.journalPhotos.forEach { photo ->
            require(photo.journalId in journalIds) {
                "A journal photo references a missing journal entry."
            }
        }
    }

    /**
     * Validates that every attachment and journal photo
     * references a real file extracted from the archive.
     */
    fun validateBackupFiles(
        backup: VaultNoteBackup,
        extractedFiles: Map<String, File>
    ) {

        val referencedEntries = mutableSetOf<String>()

        backup.attachments.forEach { attachment ->

            validateArchiveFile(
                attachment.fileEntryName,
                "attachments",
                extractedFiles,
                referencedEntries
            )
        }

        backup.journalPhotos.forEach { photo ->

            validateArchiveFile(
                photo.fileEntryName,
                "journal_photos",
                extractedFiles,
                referencedEntries
            )
        }

        // Reject unexpected or unreferenced archive files.
        require(referencedEntries == extractedFiles.keys) {
            "Backup archive contains missing or unexpected file entries."
        }
    }

    private fun validateArchiveFile(
        entryName: String,
        expectedFolder: String,
        extractedFiles: Map<String, File>,
        referencedEntries: MutableSet<String>
    ) {

        val prefix = "$expectedFolder/"

        require(entryName.startsWith(prefix)) {
            "Invalid backup attachment path."
        }

        val fileName = entryName.removePrefix(prefix)

        require(
            fileName.isNotBlank() &&
                    !fileName.contains("/") &&
                    !fileName.contains("\\") &&
                    fileName != "." &&
                    fileName != ".." &&
                    !fileName.contains("..")
        ) {
            "Unsafe backup attachment path."
        }

        require(referencedEntries.add(entryName)) {
            "Duplicate attachment file reference detected."
        }

        val file = extractedFiles[entryName]

        require(file != null && file.exists() && file.isFile) {
            "A referenced backup attachment is missing."
        }
    }

    private fun <T> requireUnique(
        values: List<T>,
        description: String
    ) {
        require(values.size == values.toSet().size) {
            "Duplicate $description IDs detected in backup."
        }
    }
}
