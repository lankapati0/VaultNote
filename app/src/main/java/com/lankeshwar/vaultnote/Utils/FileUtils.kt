package com.lankeshwar.vaultnote.Utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
object FileUtils{
    fun getDisplayFileName(context: Context,uri: Uri): String{
        var fileName: String? = null
        context.contentResolver.query(uri,
            arrayOf(OpenableColumns.DISPLAY_NAME), null,null,null)
            ?.use { cursor ->
                if(cursor.moveToFirst()){
                    val index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if(index >=0){
                        fileName = cursor.getString(index)
                    }
                }
            }
        return fileName ?: uri.lastPathSegment ?: "Selected file"
    }
}
