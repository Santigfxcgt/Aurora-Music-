package com.example.service

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicScanner {

    fun getDefaultDemoSongs(context: Context): List<Song> {
        // Library must start empty if no local music exists - no fake or demo songs.
        return emptyList()
    }

    suspend fun scanDeviceAudio(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)
                    val year = cursor.getInt(yearCol)
                    val dateAddedSec = cursor.getLong(dateAddedCol)

                    if (duration < 5000) continue // Skip sound effects < 5 seconds

                    val songId = "device_$id"
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    if (SongDeletionManager.isSongHidden(context, songId, contentUri.toString())) {
                        continue
                    }

                    val embeddedArt = ArtworkExtractor.getOrExtractEmbeddedArt(
                        context, songId, contentUri, albumId
                    )

                    val albumArtUri = embeddedArt ?: if (albumId > 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null

                    val addedAt = getOrPersistAddedAt(context, songId, dateAddedSec)

                    songList.add(
                        Song(
                            id = songId,
                            title = title,
                            artist = if (artist == "<unknown>") "Artista desconocido" else artist,
                            album = if (album == "<unknown>") "Álbum desconocido" else album,
                            durationMs = duration,
                            contentUri = contentUri.toString(),
                            albumArtUri = albumArtUri,
                            folder = dataPath.substringBeforeLast('/', "Música"),
                            year = if (year > 0) year else 2024,
                            isLocalDeviceFile = true,
                            addedAt = addedAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songList
    }

    suspend fun parseAudioFileFromUri(context: Context, uri: Uri): Song? = withContext(Dispatchers.IO) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringBeforeLast('.')
                ?: "Canción importada"

            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Artista desconocido"

            val album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "Álbum desconocido"

            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 180000L

            retriever.release()

            val songId = "imported_${uri.toString().hashCode()}"
            val embeddedArt = ArtworkExtractor.getOrExtractEmbeddedArt(context, songId, uri, null)
            val addedAt = getOrPersistAddedAt(context, songId, null)

            Song(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                contentUri = uri.toString(),
                albumArtUri = embeddedArt,
                isLocalDeviceFile = true,
                addedAt = addedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getOrPersistAddedAt(context: Context, songId: String, mediaStoreDateAddedSec: Long?): Long {
        val prefs = context.getSharedPreferences("aurora_song_added_timestamps", Context.MODE_PRIVATE)
        val existing = prefs.getLong(songId, -1L)
        if (existing > 0L) {
            return existing
        }
        val timestamp = if (mediaStoreDateAddedSec != null && mediaStoreDateAddedSec > 0L) {
            mediaStoreDateAddedSec * 1000L
        } else {
            System.currentTimeMillis()
        }
        prefs.edit().putLong(songId, timestamp).apply()
        return timestamp
    }
}
