package com.lankeshwar.vaultnote.data.storage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AttachmentFileManager(context: Context){

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

}