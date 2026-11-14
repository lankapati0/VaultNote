package com.lankeshwar.vaultnote.Utils.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dev.ffmpegkit.tesseract.TesseractOCR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class OcrTextExtractor(private val context: Context) {
    private val tesseractHelper = TesseractOcrHelper()
    private val languageManager = OcrLanguageManager()

    suspend fun extractText(uri: Uri, onProgress: (Int) -> Unit = {}): String = withContext(Dispatchers.IO) {

        val fallbackResult = extractWithFallback(uri, onProgress)
        if(fallbackResult != null && fallbackResult.text.isNotBlank()){
            return@withContext fallbackResult.text
        }
        ""
    }

    private suspend fun extractWithFallback(uri: Uri,onProgress:(Int) -> Unit): TesseractOcrResult? {
        val candidateLanguages = listOf(
            "eng",
            "hin", "mar", "nep", "san",
            "ben", "asm",
            "guj",
            "kan",
            "mal",
            "ori",
            "pan",
            "tam",
            "tel",
            "urd", "snd"
        )

        var bestResult: TesseractOcrResult? = null

        val installedLanguages = languageManager.getInstalledLanguages(context)
        for(group in candidateLanguages){
            if (group !in installedLanguages) continue
            val result = runCatching {
                tesseractHelper.extractText(context,uri,listOf(group,"eng"))
            }.getOrNull() ?: continue
            if(result.text.isNotBlank() && (bestResult == null || result.confidence > bestResult.confidence)){
                bestResult = result
            }

        }
        return bestResult
    }

    suspend fun testEnglishOcr(uri: Uri): String  = withContext(Dispatchers.IO){
        val testContext = context.createDeviceProtectedStorageContext()
        val modelFile = File(testContext.cacheDir,"eng_best.traineddata")

        var bitmap: Bitmap? = null
        try{
            val url = URL("https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/eng.traineddata")

            val connection = url.openConnection() as HttpURLConnection

            try{
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext "English best model download failed"
                }

                connection.inputStream.use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                connection.disconnect()
            }
            val installer = TesseractOCR()
            try {
                installer.initialize(testContext, language = "eng")
                installer.addLanguage(testContext,"eng",modelFile)
            } finally {
                installer.release()
            }
            modelFile.delete()

            bitmap = testContext.contentResolver.openInputStream(uri)?.use{ BitmapFactory.decodeStream(it) } ?: return@withContext "Could not open image"

            val ocr = TesseractOCR()
            try {
                ocr.initialize(testContext, language = "eng")
                val result = ocr.recognize(bitmap!!)
                val output = "BEST MODEL CONFIDENCE: ${result.confidence}\n\n${result.text}"

                output
            } finally {
                ocr.release()
            }
        } finally {
            bitmap?.recycle()
            modelFile.delete()
        }
    }
}

//ToDo: GoogleSignIn and other login need to add after completion of this OCR Work

//ToDo: when we type something and cancel without save in note, and again open create note to create new things, receive old unstored content, here when i click cancel those values also need to remove when open it again