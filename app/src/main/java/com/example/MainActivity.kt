package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.model.LibraryCategory
import com.example.model.Song
import com.example.service.AuroraMusicService
import com.example.service.CustomCoverManager
import com.example.service.MusicPlayerController
import com.example.service.PermissionHelper
import com.example.service.SongDeletionManager
import com.example.ui.components.CoverCustomizerDialog
import com.example.ui.components.DeleteActionType
import com.example.ui.components.DeleteSongDialog
import com.example.ui.components.LyricsView
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.QueueDialog
import com.example.ui.components.SearchDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SongsScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuroraMusicTheme
import com.example.ui.theme.LiquidAmbientBackground
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var controller: MusicPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        CustomCoverManager.init(applicationContext)
        controller = MusicPlayerController(applicationContext, lifecycleScope)

        setContent {
            AuroraMusicTheme {
                AuroraMusicApp(controller = controller)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionHelper.hasAudioPermission(this)) {
            controller.refreshLocalSongs()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            controller.release()
            AuroraMusicService.activeController = null
        }
        super.onDestroy()
    }
}

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    SONGS("Canciones", Icons.Default.MusicNote),
    LIBRARY("Biblioteca", Icons.Default.LibraryMusic)
}

@Composable
fun AuroraMusicApp(controller: MusicPlayerController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var activeCategory by remember { mutableStateOf<LibraryCategory?>(null) }

    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCoverCustomizerDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    var hasAudioPermission by remember {
        mutableStateOf(PermissionHelper.hasAudioPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        PermissionHelper.markPrompted(context)
        if (isGranted) {
            controller.refreshLocalSongs()
            Toast.makeText(context, "Permiso concedido. Escaneando canciones...", Toast.LENGTH_SHORT).show()
        }
    }

    val audioImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            controller.importAudioFile(uri)
            Toast.makeText(context, "Canción importada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            songToDelete?.let { song ->
                controller.onSongRemovedFromApp(song)
                Toast.makeText(context, "Archivo eliminado del dispositivo", Toast.LENGTH_SHORT).show()
            }
        }
        songToDelete = null
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission && !PermissionHelper.hasBeenPrompted(context)) {
            permissionLauncher.launch(PermissionHelper.getRequiredAudioPermission())
        } else if (hasAudioPermission) {
            controller.refreshLocalSongs()
        }
    }

    BackHandler(
        enabled = showNowPlayingSheet || showQueueDialog || showLyricsView ||
                showSearchDialog || showSettingsDialog || showCoverCustomizerDialog ||
                songToDelete != null || activeCategory != null
    ) {
        when {
            showCoverCustomizerDialog -> showCoverCustomizerDialog = false
            showLyricsView -> showLyricsView = false
            showQueueDialog -> showQueueDialog = false
            showSearchDialog -> showSearchDialog = false
            showSettingsDialog -> showSettingsDialog = false
            songToDelete != null -> songToDelete = null
            showNowPlayingSheet -> showNowPlayingSheet = false
            activeCategory != null -> activeCategory = null
        }
    }

    val allSongs by controller.allSongs.collectAsState()
    val currentSong by controller.currentSong.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val currentPositionMs by controller.currentPositionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()
    val queue by controller.queue.collectAsState()
    val currentQueueIndex by controller.currentQueueIndex.collectAsState()
    val recentlyPlayed by controller.recentlyPlayed.collectAsState()
    val isShuffleEnabled by controller.isShuffleEnabled.collectAsState()
    val repeatMode by controller.repeatMode.collectAsState()

    LiquidAmbientBackground(
        currentSong = currentSong,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    if (currentSong != null && !showNowPlayingSheet) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onTogglePlayPause = { controller.togglePlayPause() },
                            onPlayNext = { controller.playNext() },
                            onPlayPrevious = { controller.playPrevious() },
                            onClick = { showNowPlayingSheet = true }
                        )
                    }

                    LiquidBottomNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { tab ->
                            activeCategory = null
                            currentTab = tab
                        }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                activeCategory != null -> {
                    CategoryDetailScreen(
                        category = activeCategory!!,
                        songs = allSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onBackClick = { activeCategory = null },
                        onSongClick = { song -> controller.playSong(song) }
                    )
                }
                currentTab == NavTab.HOME -> {
                    HomeScreen(
                        recentlyPlayed = recentlyPlayed,
                        allSongs = allSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song -> controller.playSong(song) },
                        onOpenSearch = { showSearchDialog = true },
                        onOpenSettings = { showSettingsDialog = true },
                        onNavigateToSongs = { currentTab = NavTab.SONGS }
                    )
                }
                currentTab == NavTab.SONGS -> {
                    SongsScreen(
                        songs = allSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(PermissionHelper.getRequiredAudioPermission())
                        },
                        onScanAudio = {
                            if (hasAudioPermission) {
                                controller.refreshLocalSongs()
                            } else {
                                permissionLauncher.launch(PermissionHelper.getRequiredAudioPermission())
                            }
                        },
                        onImportAudio = { audioImportLauncher.launch("audio/*") },
                        onSongClick = { song -> controller.playSong(song) },
                        onPlayNext = { song -> controller.playNextInQueue(song) },
                        onAddToQueue = { song -> controller.addToQueue(song) },
                        onToggleFavorite = { songId -> controller.toggleFavorite(songId) },
                        onDeleteSong = { song -> songToDelete = song },
                        onOpenSearch = { showSearchDialog = true },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                currentTab == NavTab.LIBRARY -> {
                    LibraryScreen(
                        songs = allSongs,
                        onCategoryClick = { category -> activeCategory = category },
                        onOpenSearch = { showSearchDialog = true },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
            }
        }
    }

    if (showNowPlayingSheet) {
        NowPlayingSheet(
            song = currentSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            onTogglePlayPause = { controller.togglePlayPause() },
            onPlayNext = { controller.playNext() },
            onPlayPrevious = { controller.playPrevious() },
            onSeekTo = { pos -> controller.seekTo(pos) },
            onToggleShuffle = { controller.toggleShuffle() },
            onToggleRepeat = { controller.toggleRepeat() },
            onToggleFavorite = { songId -> controller.toggleFavorite(songId) },
            onOpenQueue = { showQueueDialog = true },
            onOpenLyrics = { showLyricsView = true },
            onOpenCoverCustomizer = { showCoverCustomizerDialog = true },
            onDismiss = { showNowPlayingSheet = false }
        )
    }

    if (showQueueDialog) {
        QueueDialog(
            queue = queue,
            currentQueueIndex = currentQueueIndex,
            onSongClick = { index ->
                if (index in queue.indices) {
                    controller.playSong(queue[index])
                }
            },
            onRemoveFromQueue = { index -> controller.removeFromQueue(index) },
            onDismiss = { showQueueDialog = false }
        )
    }

    if (showLyricsView) {
        LyricsView(
            song = currentSong,
            onDismiss = { showLyricsView = false }
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            allSongs = allSongs,
            onSongClick = { song -> controller.playSong(song) },
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onScanAudio = {
                if (hasAudioPermission) {
                    controller.refreshLocalSongs()
                } else {
                    permissionLauncher.launch(PermissionHelper.getRequiredAudioPermission())
                }
            },
            onImportAudio = { audioImportLauncher.launch("audio/*") },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showCoverCustomizerDialog) {
        CoverCustomizerDialog(
            song = currentSong,
            onDismiss = { showCoverCustomizerDialog = false }
        )
    }

    songToDelete?.let { song ->
        DeleteSongDialog(
            song = song,
            onConfirmDelete = { actionType ->
                when (actionType) {
                    DeleteActionType.HIDE_FROM_APP -> {
                        controller.hideSongFromLibrary(song)
                        Toast.makeText(context, "Canción quitada de Aurora Music", Toast.LENGTH_SHORT).show()
                        songToDelete = null
                    }
                    DeleteActionType.DELETE_FILE -> {
                        scope.launch {
                            when (val result = SongDeletionManager.requestPhysicalDelete(context, song)) {
                                is SongDeletionManager.PhysicalDeleteResult.Success -> {
                                    controller.onSongRemovedFromApp(song)
                                    Toast.makeText(context, "Archivo eliminado del dispositivo", Toast.LENGTH_SHORT).show()
                                    songToDelete = null
                                }
                                is SongDeletionManager.PhysicalDeleteResult.RequiresIntentSender -> {
                                    try {
                                        intentSenderLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(result.intentSender).build()
                                        )
                                    } catch (e: Exception) {
                                        controller.hideSongFromLibrary(song)
                                        Toast.makeText(context, "Canción quitada de Aurora Music", Toast.LENGTH_SHORT).show()
                                        songToDelete = null
                                    }
                                }
                                is SongDeletionManager.PhysicalDeleteResult.Error -> {
                                    controller.hideSongFromLibrary(song)
                                    Toast.makeText(context, "Canción quitada de Aurora Music", Toast.LENGTH_SHORT).show()
                                    songToDelete = null
                                }
                            }
                        }
                    }
                }
            },
            onDismiss = { songToDelete = null }
        )
    }
    }
}

@Composable
fun LiquidBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = Color(0x33161824),
        borderColor = Color(0x33FFFFFF),
        elevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val tint by animateColorAsState(
                    if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                    label = "tabTint"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = tint
                    )
                }
            }
        }
    }
}

