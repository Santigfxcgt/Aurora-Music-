package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
    song: Song?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (song == null) return

    val lyricsRaw = song.lyrics ?: "No hay letra disponible para esta canción."
    val lines = parseLrc(lyricsRaw)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161824).copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "LETRA",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lines) { _, line ->
                    Text(
                        text = line.text,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

private fun parseLrc(raw: String): List<LyricLine> {
    val result = mutableListOf<LyricLine>()
    val lines = raw.lines()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("[")) {
            val endIdx = trimmed.indexOf("]")
            if (endIdx > 1) {
                val timeStr = trimmed.substring(1, endIdx)
                val text = trimmed.substring(endIdx + 1).trim()
                val ms = parseTimestampToMs(timeStr)
                if (text.isNotBlank()) {
                    result.add(LyricLine(ms, text))
                }
            }
        } else if (trimmed.isNotBlank()) {
            result.add(LyricLine(0L, trimmed))
        }
    }
    return if (result.isEmpty()) listOf(LyricLine(0L, raw)) else result
}

private fun parseTimestampToMs(timeStr: String): Long {
    try {
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            val min = parts[0].toLong()
            val secParts = parts[1].split(".")
            val sec = secParts[0].toLong()
            val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
            return (min * 60 + sec) * 1000 + ms
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return 0L
}
