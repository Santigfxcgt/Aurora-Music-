package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LibraryCategory
import com.example.model.Song
import com.example.ui.theme.liquidGlass

@Composable
fun LibraryScreen(
    songs: List<Song>,
    onCategoryClick: (LibraryCategory) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        LibraryCategory.FAVORITES to Icons.Default.Favorite,
        LibraryCategory.ALBUMS to Icons.Default.Album,
        LibraryCategory.ARTISTS to Icons.Default.Person,
        LibraryCategory.PLAYLISTS to Icons.Default.LibraryMusic,
        LibraryCategory.FOLDERS to Icons.Default.Folder,
        LibraryCategory.GENRES to Icons.Default.MusicNote,
        LibraryCategory.QUEUE to Icons.Default.QueueMusic
    )

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
                    text = "BIBLIOTECA",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Explorar contenido",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(categories) { (category, icon) ->
                val count = when (category) {
                    LibraryCategory.FAVORITES -> songs.count { it.isFavorite }
                    LibraryCategory.ALBUMS -> songs.map { it.album }.distinct().size
                    LibraryCategory.ARTISTS -> songs.map { it.artist }.distinct().size
                    LibraryCategory.GENRES -> songs.map { it.genre }.distinct().size
                    LibraryCategory.FOLDERS -> songs.map { it.folder }.distinct().size
                    else -> songs.size
                }

                CategoryRowItem(
                    category = category,
                    icon = icon,
                    count = count,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRowItem(
    category: LibraryCategory,
    icon: ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .liquidGlass(
                cornerRadius = 20.dp,
                backgroundColor = Color(0x1AFFFFFF)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (category == LibraryCategory.FAVORITES) Color(0xFFFF4081) else Color(0xFF00E5FF),
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "$count elementos",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f)
        )
    }
}
