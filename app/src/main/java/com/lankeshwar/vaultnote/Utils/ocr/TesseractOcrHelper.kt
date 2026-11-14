package com.lankeshwar.vaultnote.Utils.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import dev.ffmpegkit.tesseract.TesseractOCR
import androidx.core.graphics.createBitmap
import dev.ffmpegkit.tesseract.TesseractConfig

data class TesseractOcrResult(val text: String, val confidence:Int)
class TesseractOcrHelper {

    suspend fun extractText(
        context: Context,
        uri: Uri,
        language: List<String>
    ): TesseractOcrResult {

        val bitmap = context.contentResolver
            .openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it) }
            ?: return TesseractOcrResult("", 0)

        val ocr = TesseractOCR()
        val processedBitmaps = mutableListOf<Bitmap>()

        try {
            ocr.initialize(
                context,
                language = language.joinToString(separator = "+"),
                config = TesseractConfig(pageSegMode = 11)
            )


            var bestResult = TesseractOcrResult("", 0)

            val originalResult = ocr.recognize(bitmap)
            if(originalResult.text.isNotBlank()){
                bestResult = TesseractOcrResult(text=originalResult.text.trim(),
                    confidence = originalResult.confidence)
            }
/*            if(bestResult.confidence >= 80){
                return bestResult
            }*/

            val grayscale = preprocessBitmap(bitmap, invert = false)
            processedBitmaps.add(grayscale)

            val grayscaleResult = ocr.recognize(grayscale)

            if(grayscaleResult.text.isNotBlank() && grayscaleResult.confidence>bestResult.confidence){
                bestResult = TesseractOcrResult(text=grayscaleResult.text.trim(),
                    confidence = grayscaleResult.confidence)
            }

/*            if(bestResult.confidence >=80){
                return bestResult
            }*/

            val inverted = preprocessBitmap(bitmap,invert = true)
            processedBitmaps.add(inverted)
            val invertedResult = ocr.recognize(inverted)

            if (invertedResult.text.isNotBlank() &&
                invertedResult.confidence > bestResult.confidence
            ) {
                bestResult = TesseractOcrResult(
                    text = invertedResult.text.trim(),
                    confidence = invertedResult.confidence
                )
            }

            return bestResult

        } finally {
            processedBitmaps.forEach { it.recycle() }
            ocr.release()
            bitmap.recycle()
        }
    }


    private fun preprocessBitmap(source: Bitmap,invert:Boolean): Bitmap {
        val maxDimension = maxOf(source.width, source.height)

        val scale = if (maxDimension < 2000) {
            minOf(2f, 2000f / maxDimension)
        } else {
            1f
        }

        val width = (source.width * scale).toInt()
        val height = (source.height * scale).toInt()

        val output = createBitmap(width, height)

        val grayscale = ColorMatrix().apply {
            setSaturation(0f)
        }

        val contrast = 1.25f
        val offset = (1f - contrast) * 127.5f

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        grayscale.postConcat(contrastMatrix)

        if (invert) {
            val inversionMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            grayscale.postConcat(inversionMatrix)
        }

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
        ).apply {
            colorFilter = ColorMatrixColorFilter(grayscale)
        }

        Canvas(output).drawBitmap(
            source,
            null,
            Rect(0, 0, width, height),
            paint
        )

        return output
    }


}