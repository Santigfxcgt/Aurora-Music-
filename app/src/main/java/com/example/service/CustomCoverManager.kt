package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class CoverType {
    IMAGE,
    GIF,
    VIDEO
}

data class CustomCover(
    val songId: String,
    val fileUri: String,
    val type: CoverType,
    val thumbnailUri: String? = null
)

object CustomCoverManager {
    private const val PREFS_NAME = "aurora_custom_covers"
    private const val KEY_COVERS_JSON = "covers_json"

    private val _customCovers = MutableStateFlow<Map<String, CustomCover>>(emptyMap())
    val customCovers: StateFlow<Map<String, CustomCover>> = _customCovers.asStateFlow()

    @Volatile
    private var cachedGifLoader: ImageLoader? = null

    fun getGifImageLoader(context: Context): ImageLoader {
        return cachedGifLoader ?: synchronized(this) {
            cachedGifLoader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build().also { cachedGifLoader = it }
        }
    }

    fun init(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_COVERS_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val json = JSONObject(jsonStr)
                val map = mutableMapOf<String, CustomCover>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val songId = keys.next()
                    val obj = json.getJSONObject(songId)
                    val uri = obj.getString("uri")
                    val typeStr = obj.optString("type", "IMAGE")
                    val thumb = if (obj.has("thumb") && !obj.isNull("thumb")) obj.getString("thumb") else null
                    val type = try {
                        CoverType.valueOf(typeStr)
                    } catch (e: Exception) {
                        CoverType.IMAGE
                    }
                    map[songId] = CustomCover(songId, uri, type, thumb)
                }
                _customCovers.value = map
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveCustomCover(
        context: Context,
        songId: String,
        sourceUri: Uri
    ): Result<CustomCover> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(sourceUri) ?: getMimeTypeFromUri(sourceUri)

            val isVideo = (mimeType != null && mimeType.startsWith("video/")) ||
                    sourceUri.toString().endsWith(".mp4", ignoreCase = true) ||
                    sourceUri.toString().endsWith(".webm", ignoreCase = true)

            val isGif = mimeType == "image/gif" || sourceUri.toString().endsWith(".gif", ignoreCase = true)

            val coverType = when {
                isVideo -> CoverType.VIDEO
                isGif -> CoverType.GIF
                else -> CoverType.IMAGE
            }

            val coversDir = File(context.filesDir, "custom_covers").apply { if (!exists()) mkdirs() }
            val ext = when (coverType) {
                CoverType.VIDEO -> if (mimeType?.contains("webm") == true) ".webm" else ".mp4"
                CoverType.GIF -> ".gif"
                CoverType.IMAGE -> ".jpg"
            }
            val destFile = File(coversDir, "cover_${songId}_${System.currentTimeMillis()}$ext")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            var thumbnailUri: String? = null
            if (coverType == CoverType.VIDEO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(destFile.absolutePath)
                    val bitmap = retriever.getFrameAtTime(1000000) ?: retriever.frameAtTime
                    if (bitmap != null) {
                        val thumbFile = File(coversDir, "thumb_${songId}_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        thumbnailUri = Uri.fromFile(thumbFile).toString()
                    }
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val customCover = CustomCover(
                songId = songId,
                fileUri = Uri.fromFile(destFile).toString(),
                type = coverType,
                thumbnailUri = thumbnailUri
            )

            val updated = _customCovers.value.toMutableMap()
            updated[songId] = customCover
            _customCovers.value = updated
            persistCovers(context)

            Result.success(customCover)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun resetCover(context: Context, songId: String) = withContext(Dispatchers.IO) {
        removeCustomCover(context, songId)
    }

    fun removeCustomCover(context: Context, songId: String) {
        val existing = _customCovers.value[songId]
        if (existing != null) {
            try {
                Uri.parse(existing.fileUri).path?.let { File(it).delete() }
                existing.thumbnailUri?.let { Uri.parse(it).path?.let { p -> File(p).delete() } }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val updated = _customCovers.value.toMutableMap()
            updated.remove(songId)
            _customCovers.value = updated
            persistCovers(context)
        }
    }

    private fun persistCovers(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = JSONObject()
        _customCovers.value.forEach { (id, cover) ->
            val obj = JSONObject().apply {
                put("uri", cover.fileUri)
                put("type", cover.type.name)
                put("thumb", cover.thumbnailUri)
            }
            json.put(id, obj)
        }
        prefs.edit().putString(KEY_COVERS_JSON, json.toString()).apply()
    }

    private fun getMimeTypeFromUri(uri: Uri): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return if (extension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
        } else null
    }
}
