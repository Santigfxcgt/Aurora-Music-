package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ArtworkExtractor {

    suspend fun getOrExtractEmbeddedArt(
        context: Context,
        songId: String,
        contentUri: Uri,
        albumId: Long? = null
    ): String? = withContext(Dispatchers.IO) {
        val coversDir = File(context.cacheDir, "embedded_covers").apply { if (!exists()) mkdirs() }
        val targetFile = File(coversDir, "cover_${songId}.jpg")

        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext Uri.fromFile(targetFile).toString()
        }

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, contentUri)
            val embeddedPicture = retriever.embeddedPicture
            retriever.release()

            if (embeddedPicture != null && embeddedPicture.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                if (bitmap != null) {
                    FileOutputStream(targetFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    return@withContext Uri.fromFile(targetFile).toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    fun cleanArtwork(context: Context, songId: String) {
        try {
            val coversDir = File(context.cacheDir, "embedded_covers")
            val targetFile = File(coversDir, "cover_${songId}.jpg")
            if (targetFile.exists()) {
                targetFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
