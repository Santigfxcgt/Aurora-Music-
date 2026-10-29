package com.example.service

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SongDeletionManager {
    private const val PREFS_NAME = "aurora_song_management"
    private const val KEY_HIDDEN_SONG_IDS = "hidden_song_ids"
    private const val KEY_HIDDEN_CONTENT_URIS = "hidden_content_uris"

    sealed class PhysicalDeleteResult {
        object Success : PhysicalDeleteResult()
        data class RequiresIntentSender(val intentSender: IntentSender) : PhysicalDeleteResult()
        data class Error(val message: String) : PhysicalDeleteResult()
    }

    fun isSongHidden(context: Context, songId: String, contentUri: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hiddenIds = prefs.getStringSet(KEY_HIDDEN_SONG_IDS, emptySet()) ?: emptySet()
        val hiddenUris = prefs.getStringSet(KEY_HIDDEN_CONTENT_URIS, emptySet()) ?: emptySet()
        return hiddenIds.contains(songId) || (contentUri.isNotBlank() && hiddenUris.contains(contentUri))
    }

    fun hideFromLibrary(context: Context, song: Song) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hiddenIds = (prefs.getStringSet(KEY_HIDDEN_SONG_IDS, emptySet()) ?: emptySet()).toMutableSet()
        val hiddenUris = (prefs.getStringSet(KEY_HIDDEN_CONTENT_URIS, emptySet()) ?: emptySet()).toMutableSet()

        hiddenIds.add(song.id)
        if (song.contentUri.isNotBlank()) {
            hiddenUris.add(song.contentUri)
        }

        prefs.edit()
            .putStringSet(KEY_HIDDEN_SONG_IDS, hiddenIds)
            .putStringSet(KEY_HIDDEN_CONTENT_URIS, hiddenUris)
            .apply()
    }

    suspend fun requestPhysicalDelete(context: Context, song: Song): PhysicalDeleteResult = withContext(Dispatchers.IO) {
        if (song.contentUri.isBlank()) {
            hideFromLibrary(context, song)
            return@withContext PhysicalDeleteResult.Success
        }

        val uri = Uri.parse(song.contentUri)
        val resolver: ContentResolver = context.contentResolver

        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
                return@withContext PhysicalDeleteResult.RequiresIntentSender(pendingIntent.intentSender)
            } else {
                val deleted = resolver.delete(uri, null, null)
                if (deleted > 0) {
                    hideFromLibrary(context, song)
                    return@withContext PhysicalDeleteResult.Success
                } else {
                    hideFromLibrary(context, song)
                    return@withContext PhysicalDeleteResult.Success
                }
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= 29) {
                val recoverableException = e as? android.app.RecoverableSecurityException
                if (recoverableException != null) {
                    return@withContext PhysicalDeleteResult.RequiresIntentSender(
                        recoverableException.userAction.actionIntent.intentSender
                    )
                }
            }
            hideFromLibrary(context, song)
            return@withContext PhysicalDeleteResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            hideFromLibrary(context, song)
            return@withContext PhysicalDeleteResult.Success
        }
    }
}
