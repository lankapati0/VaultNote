package com.lankeshwar.vaultnote.viewmodels.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lankeshwar.vaultnote.Utils.CloudSyncPreferences
import com.lankeshwar.vaultnote.data.db.dao.NoteDao
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import com.lankeshwar.vaultnote.data.model.firebase.CloudNote
import com.lankeshwar.vaultnote.data.repository.NoteAttachmentRepository
import com.lankeshwar.vaultnote.data.storage.AttachmentFileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val noteDao: NoteDao,
    @ApplicationContext private val context: Context,
    private val noteAttachmentRepository: NoteAttachmentRepository,
    private val attachmentFileManager: AttachmentFileManager
){
    private fun userDocument(userId:String): DocumentReference {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return firestore.collection("users").document(userId)
    }

    fun noteCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("notes")
    }

    fun tasksCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("tasks")
    }

    fun journalsCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("journals")
    }

    fun tagsCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("tags")
    }

    fun noteTagRelationshipsCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("noteTagRelationships")
    }

    fun noteAttachmentsCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("noteAttachments")
    }

    fun journalPhotosCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("journalPhotos")
    }

    fun syncMetadataDocument(userId: String): DocumentReference {
        return userDocument(userId).collection("syncMetadata").document("status")
    }

    suspend fun uploadNotes(userId: String,note: CloudNote){
        require(firebaseAuth.currentUser?.uid == userId) {
            "User authentication mismatch"
        }

        require(note.cloudId.isNotBlank()) {
            "Note cloud ID cannot be empty"
        }

        require(note.cloudOwnerId == userId) {
            "Note owner mismatch"
        }

        require(!note.isCloudDeleted) {
            "Cannot upload a permanently deleted note"
        }

        notesCollection(userId).document(note.cloudId).set(note).await()
    }

    fun notesCollection(userId: String): CollectionReference {
        return userDocument(userId).collection("notes")
    }

    suspend fun syncPublicNotes():Int {
        val userId = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("User is not logged in")

        var deletedCount = 0
        val deletedNotes = noteDao.getPublicNotesPendingCloudDeletion(userId)

        for(chunk in deletedNotes.chunked(450)){
            require(firebaseAuth.currentUser?.uid == userId) {
                "User authentication changed during sync"
            }

            val batch = firestore.batch()
            for (note in chunk){
                batch.set(noteCollection(userId).document(note.cloudId),
                    mapOf("isCloudDeleted" to true,
                        "updatedAt" to note.updatedAt,
                        "cloudOwnerId" to userId)
                    ,SetOptions.merge())
            }
            batch.commit().await()
            noteDao.updateNoteLastSyncedAt(
                cloudId = chunk.map { it.cloudId },
                userId = userId,
                syncedAt = System.currentTimeMillis()
            )
            deletedCount += chunk.size
        }


        val notes = noteDao.getPublicNotesForCloudSync()
        val eligibleNotes = mutableListOf<NoteEntity>()
        for(note in notes){
            if(note.isPrivate || note.isCloudDeleted) continue

            if(note.cloudOwnerId == null){
                val assigned = noteDao.assignNoteCloudOwner(
                    cloudId = note.cloudId,
                    userId = userId
                )

                if(assigned != 1) continue
            } else if(note.cloudOwnerId != userId){
                continue
            }
            eligibleNotes.add(note.copy(cloudOwnerId = userId))
        }

        if(eligibleNotes.isEmpty()) return deletedCount
        var uploadedCount = 0
        // Firestore allows a maximum of 500 writes per batch.
        for(chunk in eligibleNotes.chunked(450)){
            // Check that the same account is still logged in.
            require(firebaseAuth.currentUser?.uid == userId) {
                "User authentication changed during sync"
            }
            val batch = firestore.batch()
            for(note in chunk){
                val cloudNote = CloudNote(
                    cloudId = note.cloudId,
                    title = note.title,
                    preview = note.preview,
                    formattedContent = note.formattedContent,
                    date = note.date,
                    isPinned = note.isPinned,
                    isFavorite = note.isFavorite,
                    isDeleted = note.isDeleted,
                    deletedAt = note.deletedAt,
                    reminderTime = note.reminderTime,
                    reminderRepeat = note.reminderRepeat,
                    isReminderEnabled = note.isReminderEnabled,
                    updatedAt = note.updatedAt,
                    cloudOwnerId = userId,
                    isCloudDeleted = false
                )
                batch.set(noteCollection(userId).document(note.cloudId),cloudNote)
            }
            val startTime = System.currentTimeMillis()

            batch.commit().await()

            val uploadTime = System.currentTimeMillis() - startTime
            val syncedAt = System.currentTimeMillis()
            noteDao.updateNoteLastSyncedAt(
                cloudId = chunk.map { it.cloudId },
                userId = userId, syncedAt = syncedAt
            )
            uploadedCount += chunk.size
        }
        return uploadedCount + deletedCount
    }

    suspend fun testFirestoreWrite(): String {
        val userId = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("User is not logged in")

        val startTime = System.currentTimeMillis()

        val testData = mapOf(
            "message" to "Firestore write test",
            "createdAt" to System.currentTimeMillis()
        )

        userDocument(userId)
            .collection("syncMetadata")
            .document("write_test")
            .set(testData)
            .await()

        val elapsed = System.currentTimeMillis() - startTime

        return "Firestore write completed in ${elapsed}ms"
    }
    suspend fun downloadPublicNotes():Int {
        val userId = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User is not logged in")
        val lastDownloadTime = CloudSyncPreferences.getLastDownloadTime(context)
        // First download: get all notes.
        // Later downloads: get only recently changed notes.
        val snapshot = if(lastDownloadTime == 0L){
            notesCollection(userId = userId).get().await()
        }else {
            notesCollection(userId)
                .whereGreaterThan("updatedAt",(lastDownloadTime - 2000L).coerceAtLeast(0L))
                .orderBy("updatedAt").get().await()
        }
        var downloadedCount = 0
        var latestUpdatedAt = lastDownloadTime

        for(document in snapshot.documents){
            val cloudNote = document.toObject(CloudNote::class.java) ?: continue

            if(cloudNote.cloudOwnerId != userId) continue
            val localNote = noteDao.getNoteByCloudId(cloudNote.cloudId)
            if(cloudNote.isCloudDeleted){
                if(localNote != null && !localNote.isPrivate && localNote.cloudOwnerId == userId){
                    val attachments = noteAttachmentRepository.getAttachmentsForDeletion(localNote.id)
                    var allFilesDeleted = true
                    for(attachment in attachments){
                        val deleted = attachmentFileManager.deleteAttachment(attachment.filePath)

                        if(!deleted){
                            allFilesDeleted = false
                            break
                        }
                    }
                    if(!allFilesDeleted){
                        Log.e("CloudDownload", "Attachment cleanup failed. Note deletion skipped.")
                        break
                    }
                    noteAttachmentRepository.deleteAttachmentForNote(localNote.id)
                    noteDao.permanentlyDeleteNote(localNote.id)
                    downloadedCount++
                }
                latestUpdatedAt = maxOf(latestUpdatedAt,cloudNote.updatedAt)
                continue
            }

            if(localNote == null){
                val newNote = NoteEntity(
                    title = cloudNote.title,
                    date = cloudNote.date,
                    deletedAt = cloudNote.deletedAt,
                    cloudId = cloudNote.cloudId,
                    lastSyncedAt = cloudNote.lastSyncedAt,
                    updatedAt = cloudNote.updatedAt,
                    isPinned = cloudNote.isPinned,
                    isPrivate = false,
                    isDeleted = cloudNote.isDeleted,
                    preview = cloudNote.preview,
                    isFavorite = cloudNote.isFavorite,
                    reminderTime = cloudNote.reminderTime,
                    reminderRepeat = cloudNote.reminderRepeat,
                    formattedContent = cloudNote.formattedContent,
                    isReminderEnabled = cloudNote.isReminderEnabled,
                    cloudOwnerId = userId,
                    isCloudDeleted = false
                )

                noteDao.insertNote(newNote)
                downloadedCount++
            } else {
                if(localNote.isPrivate) continue
                if(localNote.cloudOwnerId != userId) continue


                if(cloudNote.updatedAt > localNote.updatedAt || (localNote.isDeleted && !cloudNote.isDeleted)){
                    val updatedNote = localNote.copy(
                        title = cloudNote.title,
                        preview = cloudNote.preview,
                        formattedContent = cloudNote.formattedContent,
                        date = cloudNote.date,
                        isPinned = cloudNote.isPinned,
                        isFavorite = cloudNote.isFavorite,
                        isDeleted = cloudNote.isDeleted,
                        deletedAt = cloudNote.deletedAt,
                        reminderTime = cloudNote.reminderTime,
                        reminderRepeat = cloudNote.reminderRepeat,
                        isReminderEnabled = cloudNote.isReminderEnabled,
                        updatedAt = cloudNote.updatedAt,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    noteDao.updateNote(updatedNote)
                    downloadedCount++
                }
            }
            latestUpdatedAt = maxOf(latestUpdatedAt,cloudNote.updatedAt)
        }
        CloudSyncPreferences.saveLastDownloadTime(context,latestUpdatedAt)
        return  downloadedCount
    }


}