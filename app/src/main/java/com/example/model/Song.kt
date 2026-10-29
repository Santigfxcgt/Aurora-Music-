package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val albumArtResId: Int? = null,
    val audioFormat: String = "Lossless • FLAC • 24-bit • 44.1 kHz",
    val lyrics: String? = null,
    val genre: String = "Hip-Hop",
    val folder: String = "Music",
    val year: Int = 2024,
    val isFavorite: Boolean = false,
    val isLocalDeviceFile: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun formattedDuration(): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class LibraryCategory(val title: String) {
    FAVORITES("Favoritos"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    PLAYLISTS("Listas de reproducción"),
    FOLDERS("Carpetas"),
    GENRES("Géneros"),
    QUEUE("Cola")
}
