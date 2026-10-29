package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object AmbientColorExtractor {

    suspend fun extractFromUri(context: Context, uriStr: String): LiquidPalette = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriStr)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                return@withContext extractFromBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LiquidThemeColors.Default
    }

    suspend fun extractFromResId(context: Context, resId: Int): LiquidPalette = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            if (bitmap != null) {
                return@withContext extractFromBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LiquidThemeColors.Default
    }

    fun extractFromBitmap(bitmap: Bitmap): LiquidPalette {
        val palette = Palette.from(bitmap).generate()

        val dominant = palette.getDominantColor(0xFF7C4DFF.toInt())
        val vibrant = palette.getVibrantColor(palette.getMutedColor(dominant))
        val darkVibrant = palette.getDarkVibrantColor(palette.getDarkMutedColor(0xFF0D0E15.toInt()))
        val lightVibrant = palette.getLightVibrantColor(palette.getLightMutedColor(0xFF00E5FF.toInt()))

        return LiquidPalette(
            primary = Color(vibrant),
            secondary = Color(lightVibrant),
            darkBackground = Color(darkVibrant),
            glowingAccent = Color(dominant)
        )
    }
}
