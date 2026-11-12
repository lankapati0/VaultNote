package com.lankeshwar.vaultnote.data.storage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.lankeshwar.vaultnote.Utils.PrivateAttachmentEncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentFileManager @Inject constructor(@ApplicationContext context: Context){

    private val appContext = context.applicationContext

    private val attachmentsDirectory: File get() = File(appContext.filesDir,"attachments").apply {
        if(!exists()) mkdirs()
    }

    fun copyAttachment(uri: Uri,originalFileName: String): File{
        val extension = originalFileName.substringAfterLast('.',"")
            .takeIf { it.isNotBlank() }
            ?: MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(appContext.contentResolver.getType(uri)).orEmpty()

        val safeExtension = extension.filter { it.isLetterOrDigit() }.take(10)

        val fileName = UUID.randomUUID().toString() +
                if(safeExtension.isNotBlank()) ".$safeExtension" else ""

        val destination = File(attachmentsDirectory,fileName)

        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open the selected file.")

        inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        return destination
    }

    fun deleteAttachment(filePath: String): Boolean {
        val file = File(filePath)
        val root = attachmentsDirectory.canonicalFile
        val target = file.canonicalFile

        if(!target.path.startsWith(root.path + File.separator)){
            return false
        }
        return !target.exists() || target.delete()
    }

    fun copyPrivateAttachment(uri: Uri,originalFileName: String): File{
        val extension = originalFileName.substringAfterLast('.',"").filter { it.isLetterOrDigit() }.take(5)

        val filename = UUID.randomUUID().toString() + if(extension.isNotBlank()) ".$extension" else ""
        val destination = File(attachmentsDirectory,filename)

        val temporaryPlainFile = File.createTempFile("private_attachment",".tmp",appContext.cacheDir)

        try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open selected file.")
            inputStream.use { input ->
                temporaryPlainFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            PrivateAttachmentEncryptionManager.encryptFile(temporaryPlainFile,destination)

            return destination
        } catch (e: Exception){
            destination.delete()
            throw e
        } finally {
            temporaryPlainFile.delete()
        }
    }

    fun createDecryptedTemporaryFile(encryptedFilePath:String):File{
        val encryptedFile = File(encryptedFilePath)

        val temporaryDirectory = File(appContext.cacheDir,"decrypted_attachments").apply { mkdirs() }
        val extension = encryptedFile.name.substringAfterLast('.',"")
            .takeIf { it.isNotBlank() } ?: "tmp"
        val temporaryFile = File(temporaryDirectory,"decrypted_${UUID.randomUUID()}.$extension")
        try {
            PrivateAttachmentEncryptionManager.decryptFile(encryptedFile,temporaryFile)
            return temporaryFile
        }catch (e: Exception){
            temporaryFile.delete()
            throw e
        }
    }

    fun encryptPrivateAttachmentFile(sourceFile:File): File{
        val destination = File(attachmentsDirectory,"${UUID.randomUUID()}.enc")

        try {
            PrivateAttachmentEncryptionManager.encryptFile(sourceFile,destination)
            return destination
        }catch (e: Exception){
            destination.delete()
            throw e
        }
    }

}