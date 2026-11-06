package com.lankeshwar.vaultnote.Utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import androidx.core.graphics.scale


object ImageBackupCompressor {

    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 75

    fun compressJpeg(
        source: File,
        destination: File
    ): Boolean {

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(source.absolutePath, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return false
            }

            var sampleSize = 1

            while (
                options.outWidth / sampleSize > MAX_DIMENSION * 2 ||
                options.outHeight / sampleSize > MAX_DIMENSION * 2
            ) {
                sampleSize *= 2
            }

            val bitmap = BitmapFactory.decodeFile(
                source.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
            ) ?: return false

            try {
                val scale = minOf(
                    1f,
                    MAX_DIMENSION.toFloat() /
                            maxOf(bitmap.width, bitmap.height)
                )

                val outputBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale)
                            .toInt().coerceAtLeast(1),
                        (bitmap.height * scale)
                            .toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bitmap
                }

                try {
                    destination.outputStream().buffered().use { output ->
                        outputBitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            JPEG_QUALITY,
                            output
                        )
                    }
                } finally {
                    if (outputBitmap !== bitmap) {
                        outputBitmap.recycle()
                    }
                }

                destination.exists() &&
                        destination.isFile &&
                        destination.length() > 0 &&
                        destination.length() < source.length()

            } finally {
                bitmap.recycle()
            }

        } catch (e: Exception) {
            destination.delete()
            false
        }
    }
}
