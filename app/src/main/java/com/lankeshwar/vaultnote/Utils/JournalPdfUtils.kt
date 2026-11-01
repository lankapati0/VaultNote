package com.lankeshwar.vaultnote.Utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream

object JournalPdfUtils {

    fun createJournalPdf(
        date: String,
        title: String,
        mood: String,
        content: String,
        photoPaths: List<String> = emptyList()
    ): ByteArray? {

        val pdfDocument = PdfDocument()

        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                595, 842, 1
            ).create()

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isAntiAlias = true
            }

            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 60f

            fun startNewPage() {
                page?.let { pdfDocument.finishPage(it) }

                pageNumber++

                val info = PdfDocument.PageInfo.Builder(
                    595, 842, pageNumber
                ).create()

                page = pdfDocument.startPage(info)
                canvas = page!!.canvas
                y = 60f
            }

            fun drawLine(text: String, size: Float = 16f) {
                if (y > 790f) {
                    startNewPage()
                }

                paint.textSize = size
                canvas?.drawText(text, 40f, y, paint)
                y += size + 14f
            }

            fun drawParagraph(text: String) {
                val maxWidth = 515f
                var currentLine = ""

                text.split(" ").forEach { word ->
                    val testLine = if (currentLine.isEmpty()) {
                        word
                    } else {
                        "$currentLine $word"
                    }

                    if (paint.measureText(testLine) > maxWidth &&
                        currentLine.isNotEmpty()
                    ) {
                        drawLine(currentLine)
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }

                if (currentLine.isNotEmpty()) {
                    drawLine(currentLine)
                }
            }

            fun drawPhoto(photoPath: String) {
                val bitmap = BitmapFactory.decodeFile(photoPath) ?: return

                val maxWidth = 515f
                val maxHeight = 350f

                val scale = minOf(
                    maxWidth / bitmap.width,
                    maxHeight / bitmap.height
                )

                val width = bitmap.width * scale
                val height = bitmap.height * scale

                if (y + height > 790f) {
                    startNewPage()
                }

                val destination = RectF(
                    40f,
                    y,
                    40f + width,
                    y + height
                )

                canvas?.drawBitmap(bitmap, null, destination, paint)

                y += height + 20f

                bitmap.recycle()
            }

            startNewPage()

            drawLine("Daily Journal", 24f)
            drawLine("Date: $date")
            drawLine("Title: $title")
            drawLine("Mood: ${mood.ifBlank { "Not selected" }}")

            y += 20f
            drawLine("Journal Entry", 18f)
            y += 8f

            content.lines().forEach { line ->
                if (line.isBlank()) {
                    y += 12f
                } else {
                    drawParagraph(line)
                }
            }

            if(photoPaths.isNotEmpty()){
                y += 20f
                drawLine("Attached Photos",18f)
                photoPaths.forEach { photoPath ->
                    drawPhoto(photoPath)
                }
            }

            page?.let { pdfDocument.finishPage(it) }

            val output = ByteArrayOutputStream()
            pdfDocument.writeTo(output)

            output.toByteArray()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    fun saveJournalPdf(
        context: Context,
        date: String,
        pdfBytes: ByteArray
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        return try {
            val values = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    "Journal_$date.pdf"
                )
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/pdf"
                )
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Documents/VaultNote/"
                )
                put(
                    MediaStore.MediaColumns.IS_PENDING,
                    1
                )
            }

            val resolver = context.contentResolver

            val uri = resolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            ) ?: return null

            resolver.openOutputStream(uri)?.use { output ->
                output.write(pdfBytes)
            } ?: return null

            val completedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }

            resolver.update(uri, completedValues, null, null)

            uri

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("JournalPdfUtils", "PDF saving failed", e)
            null
        }
    }

    fun shareJournalPdf(context: Context, pdfUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share Journal PDF")
        )
    }

    fun sharePdfBytes(context: Context, pdfBytes: ByteArray) {
        val file = File(context.cacheDir, "JournalShare.pdf")
        file.writeBytes(pdfBytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        shareJournalPdf(context, uri)
    }

}