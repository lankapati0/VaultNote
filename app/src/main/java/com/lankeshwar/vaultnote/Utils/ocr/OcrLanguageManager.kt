package com.lankeshwar.vaultnote.Utils.ocr
import android.content.Context
import dev.ffmpegkit.tesseract.TesseractOCR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class OcrLanguageManager {
    private val tessarect = TesseractOCR()

    val supportedLanguages = listOf(
        "afr", "amh", "ara", "asm", "aze", "bel",
        "ben", "bod", "bos", "bre", "bul", "cat",
        "ceb", "ces", "chi_sim", "chi_tra", "chr",
        "cym", "dan", "deu", "div", "dzo", "ell",
        "eng", "enm", "epo", "est", "eus", "fao",
        "fas", "fil", "fin", "fra", "frk", "frm",
        "gle", "glg", "grc", "guj", "hat", "heb",
        "hin", "hrv", "hun", "hye", "iku", "ind",
        "isl", "ita", "jav", "jpn", "kan", "kat",
        "kaz", "khm", "kir", "kmr", "kor", "lao",
        "lat", "lav", "lit", "mal", "mar", "mkd",
        "mlt", "mon", "mya", "nep", "nld",
        "nor", "oci", "ori", "pan", "pol", "por",
        "pus", "ron", "rus", "san", "sin", "slk",
        "slv", "snd", "spa", "sqi", "srp", "sun",
        "swa", "swe", "syr", "tam", "tat", "tel",
        "tgk", "tha", "tir", "ton", "tur", "uig",
        "ukr", "urd", "uzb", "vie", "yid"
    )

    val initialOcrLanguages = listOf(
        "eng", "asm", "ben", "guj", "hin", "kan", "mal", "mar", "nep", "ori", "pan", "san", "snd", "tam", "tel", "urd"
    )

    suspend fun downloadLanguage(context: Context, language: String, onProgress: (Int)-> Unit= {}): Boolean = withContext(
        Dispatchers.IO){
        // Check whether this language is already installed.
/*        if(language in tessarect.getAvailableLanguages(context)) {
            return@withContext true
        }*/
        val availableLanguages = tessarect.getAvailableLanguages(context)

        android.util.Log.d(
            "VaultNoteOCR",
            "Available models: $availableLanguages"
        )

        android.util.Log.d(
            "VaultNoteOCR",
            "Checking model: $language"
        )

        if (language in availableLanguages) {
            android.util.Log.d(
                "VaultNoteOCR",
                "$language already installed. Skipping download."
            )
            return@withContext true
        }

        android.util.Log.d(
            "VaultNoteOCR",
            "$language not found. Downloading..."
        )

        val url = URL("https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/$language.traineddata")

        val tempFile = File(context.cacheDir, "$language.traineddata")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()

            if(connection.responseCode != HttpURLConnection.HTTP_OK){
                return@withContext false
            }
            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use {input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var byteRead: Int

                    while(input.read(buffer).also { byteRead = it } != -1){
                        output.write(buffer,0,byteRead)
                        downloadedBytes += byteRead
                        if(totalBytes > 0){
                            val progress = (downloadedBytes * 100 / totalBytes).toInt().coerceIn(0,100)
                            onProgress(progress)
                        }
                    }
                    input.copyTo(output)
                }
            }

            onProgress(100)

            if(!tempFile.exists() || tempFile.length() == 0L){
                return@withContext false
            }
            tessarect.addLanguage(context,language,tempFile)

            language in tessarect.getAvailableLanguages(context)
        }finally {
            connection.disconnect()
            tempFile.delete()
        }
    }

    suspend fun downloadInitialLanguagePack(
        context: Context,onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO){
        val totalLanguages = initialOcrLanguages.size

        for((index,language) in initialOcrLanguages.withIndex()){
            val success = downloadLanguage(context,language) {fileProgress ->
                val overallProgress = ((index * 100 + fileProgress)/totalLanguages).coerceIn(0,100)
                onProgress(overallProgress,language)
            }
            if(!success){
                return@withContext false
            }
        }
        onProgress(100,"Complete")
        true
    }

    fun isInitialLanguagePackInstalled(context: Context): Boolean{
        val installedLanguages = getInstalledLanguages(context)
        return initialOcrLanguages.all { language ->
            language in installedLanguages
        }
    }

    fun getInstalledLanguages(context:Context):List<String>{
        return tessarect.getAvailableLanguages(context)
    }

    fun isLanguageInstalled(context: Context, language:String): Boolean{
        return language in tessarect.getAvailableLanguages(context)
    }

    suspend fun ensureLanguageInstalled(context: Context, languages: List<String>?,onProgress: (Int, String) -> Unit): List<String> = withContext(Dispatchers.IO){
        val installedLanguages = mutableListOf<String>()
        languages?.distinct()?.filter { it in supportedLanguages }
            ?.forEach { language ->
                val success = runCatching { downloadLanguage(context,language){progress ->
                    onProgress(progress,language)
                } }.getOrDefault(false)

                if(success){
                    installedLanguages.add(language)
                }
            }
        installedLanguages
    }
}