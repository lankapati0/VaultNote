package com.lankeshwar.vaultnote.data.storage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class JournalPhotoFileManager(context: Context) {

    private val appContext = context.applicationContext

    private val photosDirectory: File
        get() = File(appContext.filesDir,"journal_photos").apply {
            if(!exists()) mkdirs()
        }

    fun copyPhoto(uri: Uri):File{
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(appContext.contentResolver.getType(uri)).orEmpty()

        val safeExtension = extension.filter { it.isLetterOrDigit() }.take(10)

        val fileName = UUID.randomUUID().toString() + if(safeExtension.isNotBlank()) ".$safeExtension" else ""

        val destination = File(photosDirectory,fileName)

        val inputStream = appContext.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open the selected photo.")

        inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        return destination
    }

    fun deletePhoto(filePath: String): Boolean{
        val file = File(filePath)
        val root = photosDirectory.canonicalFile
        val target = file.canonicalFile

        if(!target.path.startsWith(root.path + File.separator)){
            return false
        }
        return !target.exists() || target.delete()
    }


}