package com.example.service

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
class MusicPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        setAudioAttributes(audioAttributes, true)
    }

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongIds: StateFlow<Set<String>> = _favoriteSongIds.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var progressTrackerJob: Job? = null

    init {
        val defaultSongs = MusicScanner.getDefaultDemoSongs(context)
        _allSongs.value = defaultSongs
        _queue.value = defaultSongs
        _favoriteSongIds.value = defaultSongs.filter { it.isFavorite }.map { it.id }.toSet()
        _recentlyPlayed.value = emptyList()
        _currentSong.value = defaultSongs.firstOrNull()
        _currentQueueIndex.value = if (defaultSongs.isNotEmpty()) 0 else -1
        _durationMs.value = defaultSongs.firstOrNull()?.durationMs ?: 0L

        setupExoPlayerListener()
        startProgressTracking()

        AuroraMusicService.activeController = this
        AuroraMusicService.startService(context)
    }

    private fun setupExoPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                    }
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        val d = exoPlayer.duration
                        if (d > 0) {
                            _durationMs.value = d
                        }
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                        handleSongEnded()
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                    }
                }
            }
        })
    }

    private fun startProgressTracking() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPositionMs.value = exoPlayer.currentPosition
                    val d = exoPlayer.duration
                    if (d > 0 && d != C.TIME_UNSET) {
                        _durationMs.value = d
                    }
                }
                delay(100)
            }
        }
    }

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        scope.launch {
            if (newQueue != null && newQueue.isNotEmpty()) {
                _queue.value = newQueue
                _currentQueueIndex.value = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            } else {
                val existingIdx = _queue.value.indexOfFirst { it.id == song.id }
                if (existingIdx != -1) {
                    _currentQueueIndex.value = existingIdx
                } else {
                    _queue.update { it + song }
                    _currentQueueIndex.value = _queue.value.lastIndex
                }
            }

            _currentSong.value = song
            _durationMs.value = song.durationMs
            _currentPositionMs.value = 0L

            // Record in recently played
            _recentlyPlayed.update { list ->
                val filtered = list.filter { it.id != song.id }
                listOf(song) + filtered.take(9)
            }

            val playableUri: Uri = resolvePlayableUri(song)

            val customCover = CustomCoverManager.customCovers.value[song.id]
            val artworkUri = when {
                customCover != null -> Uri.parse(customCover.thumbnailUri ?: customCover.fileUri)
                !song.albumArtUri.isNullOrBlank() -> Uri.parse(song.albumArtUri)
                song.albumArtResId != null -> Uri.parse("android.resource://${context.packageName}/${song.albumArtResId}")
                else -> null
            }

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .apply {
                    if (artworkUri != null) {
                        setArtworkUri(artworkUri)
                    }
                }
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(playableUri)
                .setMediaMetadata(mediaMetadata)
                .build()

            withContext(Dispatchers.Main) {
                try {
                    exoPlayer.stop()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                    AuroraMusicService.startService(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun resolvePlayableUri(song: Song): Uri {
        return if (song.contentUri.isNotBlank()) {
            Uri.parse(song.contentUri)
        } else {
            val file = AudioSynthesizer.getOrCreateDemoAudioFile(context, song.id)
            Uri.fromFile(file)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.currentMediaItem == null) {
                val songToPlay = _currentSong.value ?: _allSongs.value.firstOrNull()
                if (songToPlay != null) {
                    playSong(songToPlay)
                }
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1000L))
        _currentPositionMs.value = target
        exoPlayer.seekTo(target)
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_repeatMode.value == RepeatMode.ONE) {
            _currentSong.value?.let { playSong(it) }
            return
        }

        val currIndex = if (_currentQueueIndex.value in q.indices) {
            _currentQueueIndex.value
        } else {
            q.indexOfFirst { it.id == _currentSong.value?.id }.coerceAtLeast(0)
        }

        val nextIndex = if (_isShuffleEnabled.value && q.size > 1) {
            var rand = (0 until q.size).random()
            while (rand == currIndex && q.size > 1) {
                rand = (0 until q.size).random()
            }
            rand
        } else {
            if (currIndex + 1 < q.size) {
                currIndex + 1
            } else {
                0
            }
        }

        if (nextIndex in q.indices) {
            playSong(q[nextIndex])
        }
    }

    fun playPrevious() {
        if (exoPlayer.currentPosition > 3000L) {
            seekTo(0L)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        val prevIndex = if (_isShuffleEnabled.value && q.size > 1) {
            (0 until q.size).random()
        } else {
            val curr = _currentQueueIndex.value
            if (curr - 1 >= 0) {
                curr - 1
            } else {
                q.lastIndex
            }
        }

        if (prevIndex in q.indices) {
            playSong(q[prevIndex])
        }
    }

    private fun handleSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (_currentQueueIndex.value + 1 < _queue.value.size || _isShuffleEnabled.value) {
                    playNext()
                } else {
                    exoPlayer.pause()
                    seekTo(0L)
                }
            }
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.update { !it }
    }

    fun toggleRepeat() {
        _repeatMode.update { current ->
            when (current) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
        }
    }

    fun toggleFavorite(songId: String) {
        _favoriteSongIds.update { set ->
            if (set.contains(songId)) set - songId else set + songId
        }
        _allSongs.update { list ->
            list.map { if (it.id == songId) it.copy(isFavorite = !it.isFavorite) else it }
        }
        _currentSong.update { curr ->
            if (curr?.id == songId) curr.copy(isFavorite = !curr.isFavorite) else curr
        }
    }

    fun addToQueue(song: Song) {
        _queue.update { it + song }
    }

    fun playNextInQueue(song: Song) {
        _queue.update { currentList ->
            val idx = _currentQueueIndex.value
            val mutable = currentList.toMutableList()
            mutable.add((idx + 1).coerceAtMost(mutable.size), song)
            mutable
        }
    }

    fun removeFromQueue(index: Int) {
        _queue.update { list ->
            if (index in list.indices) {
                val mutable = list.toMutableList()
                mutable.removeAt(index)
                if (index < _currentQueueIndex.value) {
                    _currentQueueIndex.update { it - 1 }
                }
                mutable
            } else list
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _queue.update { list ->
            if (fromIndex in list.indices && toIndex in list.indices) {
                val mutable = list.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable
            } else list
        }
    }

    fun refreshLocalSongs() {
        scope.launch {
            val localSongs = MusicScanner.scanDeviceAudio(context)
            val songsToUse = if (localSongs.isNotEmpty()) localSongs else MusicScanner.getDefaultDemoSongs(context)
            _allSongs.value = songsToUse
            if (_queue.value.isEmpty()) {
                _queue.value = songsToUse
            }
            if (_currentSong.value == null && songsToUse.isNotEmpty()) {
                val first = songsToUse.first()
                _currentSong.value = first
                _currentQueueIndex.value = 0
                _durationMs.value = first.durationMs
            }
        }
    }

    fun hideSongFromLibrary(song: Song) {
        SongDeletionManager.hideFromLibrary(context, song)
        onSongRemovedFromApp(song)
    }

    fun onSongRemovedFromApp(song: Song) {
        scope.launch(Dispatchers.Main) {
            // If current playing song is deleted
            if (_currentSong.value?.id == song.id) {
                val remainingInQueue = _queue.value.filter { it.id != song.id }
                if (remainingInQueue.isNotEmpty()) {
                    playNext()
                } else {
                    exoPlayer.stop()
                    _currentSong.value = null
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    _durationMs.value = 0L
                }
            }

            // Remove from player lists
            _allSongs.update { list -> list.filter { it.id != song.id } }
            _queue.update { list -> list.filter { it.id != song.id } }
            _recentlyPlayed.update { list -> list.filter { it.id != song.id } }
            _favoriteSongIds.update { set -> set - song.id }

            // Clean custom cover and embedded artwork cache
            CustomCoverManager.removeCustomCover(context, song.id)
            ArtworkExtractor.cleanArtwork(context, song.id)
        }
    }

    fun importAudioFile(uri: Uri) {
        scope.launch {
            val song = MusicScanner.parseAudioFileFromUri(context, uri)
            if (song != null) {
                _allSongs.update { listOf(song) + it }
                playSong(song)
            }
        }
    }

    fun release() {
        progressTrackerJob?.cancel()
        if (AuroraMusicService.activeController == this) {
            AuroraMusicService.activeController = null
        }
        exoPlayer.release()
    }
}
