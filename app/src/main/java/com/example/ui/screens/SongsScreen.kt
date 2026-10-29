package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.components.SongArtwork
import com.example.ui.theme.liquidGlass

enum class SongSortOption(val displayName: String) {
    RECENTLY_ADDED("Agregadas recientemente"),
    TITLE("Título (A-Z)"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DURATION("Duración"),
    YEAR("Año")
}

@Composable
fun SongsScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onScanAudio: () -> Unit,
    onImportAudio: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOption by remember { mutableStateOf(SongSortOption.RECENTLY_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SongSortOption.RECENTLY_ADDED -> songs.sortedByDescending { it.addedAt }
            SongSortOption.TITLE -> songs.sortedBy { it.title.lowercase() }
            SongSortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SongSortOption.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SongSortOption.DURATION -> songs.sortedByDescending { it.durationMs }
            SongSortOption.YEAR -> songs.sortedByDescending { it.year }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CANCIONES",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "${songs.size} pistas en biblioteca",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                }

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SongSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortOption == option) Color(0xFF00E5FF) else Color.White
                                    )
                                },
                                onClick = {
                                    sortOption = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
                }
            }
        }

        if (!hasAudioPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .liquidGlass(cornerRadius = 20.dp, backgroundColor = Color(0x337C4DFF))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Acceso a música del dispositivo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Concede permiso para escanear tus canciones descargadas.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Conceder Permiso", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (sortedSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sin canciones en la biblioteca",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usa el botón para escanear archivos del dispositivo o importar audio.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onScanAudio,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear música", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                itemsIndexed(sortedSongs) { _, song ->
                    val isCurrent = currentSong?.id == song.id

                    LiquidSongListItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSongClick(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onDelete = { onDeleteSong(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidSongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val accentColor by animateColorAsState(
        targetValue = if (isCurrent) Color(0xFF00E5FF) else Color.White,
        animationSpec = tween(500),
        label = "itemAccent"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(
                cornerRadius = 16.dp,
                backgroundColor = if (isCurrent) Color(0x3300E5FF) else Color(0x1AFFFFFF),
                borderColor = if (isCurrent) Color(0x8000E5FF) else Color(0x22FFFFFF)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(
            song = song,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            allowVideoLoop = false,
            isPlaying = isPlaying
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = accentColor,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.formattedDuration()}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (song.isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.6f)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reproducir a continuación") },
                    leadingIcon = { Icon(Icons.Default.Queue, contentDescription = null) },
                    onClick = {
                        onPlayNext()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Añadir a la cola") },
                    leadingIcon = { Icon(Icons.Default.Queue, contentDescription = null) },
                    onClick = {
                        onAddToQueue()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar canción", color = Color(0xFFFF4081)) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4081)) },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}
