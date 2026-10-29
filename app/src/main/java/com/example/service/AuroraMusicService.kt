package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.InputStream

class AuroraMusicService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "aurora_music_playback"
        const val CHANNEL_NAME = "Reproducción de Aurora Music"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"

        @Volatile
        var activeController: MusicPlayerController? = null

        fun startService(context: Context) {
            try {
                val intent = Intent(context, AuroraMusicService::class.java)
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateObservationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        startInForeground()
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            when (action) {
                ACTION_PLAY -> activeController?.togglePlayPause()
                ACTION_PAUSE -> activeController?.togglePlayPause()
                ACTION_NEXT -> activeController?.playNext()
                ACTION_PREV -> activeController?.playPrevious()
            }
        }
        startInForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startInForeground() {
        val notification = buildCurrentNotification()
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildCurrentNotification(): Notification {
        val controller = activeController
        val song = controller?.currentSong?.value
        val isPlaying = controller?.isPlaying?.value ?: false

        val title = song?.title ?: "Aurora Music"
        val artist = song?.artist ?: "Reproductor de música"

        val openActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AuroraMusicService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this, 2,
            Intent(this, AuroraMusicService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 3,
            Intent(this, AuroraMusicService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var artworkBitmap: Bitmap? = null
        if (song != null) {
            try {
                val customCover = CustomCoverManager.customCovers.value[song.id]
                val thumbUriStr = customCover?.thumbnailUri ?: customCover?.fileUri ?: song.albumArtUri
                if (!thumbUriStr.isNullOrBlank()) {
                    val input: InputStream? = contentResolver.openInputStream(Uri.parse(thumbUriStr))
                    artworkBitmap = BitmapFactory.decodeStream(input)
                    input?.close()
                } else if (song.albumArtResId != null) {
                    artworkBitmap = BitmapFactory.decodeResource(resources, song.albumArtResId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Aurora Music")
            .setSmallIcon(R.drawable.aurora_app_icon)
            .setContentIntent(openActivityIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproducir",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Siguiente", nextPendingIntent)

        if (mediaSession != null) {
            val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        } else {
            val mediaStyle = MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        }

        if (artworkBitmap != null) {
            builder.setLargeIcon(artworkBitmap)
        }

        return builder.build()
    }

    private fun observePlaybackState() {
        val controller = activeController ?: return
        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            controller.currentSong.collect { updateNotification() }
        }
        serviceScope.launch {
            controller.isPlaying.collect { updateNotification() }
        }
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildCurrentNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Controles de reproducción multimedia en segundo plano"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun setupMediaSession() {
        val controller = activeController ?: return
        val player = controller.exoPlayer
        if (mediaSession != null) return

        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() {
                activeController?.playNext()
            }

            override fun seekToNextMediaItem() {
                activeController?.playNext()
            }

            override fun seekToPrevious() {
                activeController?.playPrevious()
            }

            override fun seekToPreviousMediaItem() {
                activeController?.playPrevious()
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val connectionResult = super.onConnect(session, controllerInfo)
                    val sessionCommands = connectionResult.availableSessionCommands
                    val playerCommands = connectionResult.availablePlayerCommands
                        .buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }

                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                            activeController?.playPrevious()
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                            activeController?.playNext()
                            return SessionResult.RESULT_SUCCESS
                        }
                        else -> return super.onPlayerCommandRequest(session, controllerInfo, playerCommand)
                    }
                }

                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK -> {
                                activeController?.togglePlayPause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                activeController?.playNext()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                activeController?.playPrevious()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                activeController?.let { if (!it.isPlaying.value) it.togglePlayPause() }
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                activeController?.let { if (it.isPlaying.value) it.togglePlayPause() }
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(session, controllerInfo, intent)
                }
            })
            .setId("AuroraMusicSession")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null) {
            setupMediaSession()
        }
        return mediaSession
    }

    override fun onDestroy() {
        stateObservationJob?.cancel()
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
