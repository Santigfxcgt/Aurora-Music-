package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.liquidGlass

enum class DeleteActionType {
    HIDE_FROM_APP,
    DELETE_FILE
}

@Composable
fun DeleteSongDialog(
    song: Song,
    onConfirmDelete: (DeleteActionType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(DeleteActionType.HIDE_FROM_APP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161824),
        title = {
            Text(
                text = "Eliminar canción",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "¿Cómo deseas proceder con \"${song.title}\"?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            cornerRadius = 12.dp,
                            backgroundColor = if (selectedType == DeleteActionType.HIDE_FROM_APP) Color(0x3300E5FF) else Color(0x1AFFFFFF)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == DeleteActionType.HIDE_FROM_APP,
                        onClick = { selectedType = DeleteActionType.HIDE_FROM_APP },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Quitar de Aurora Music", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Mantiene el archivo en el dispositivo", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            cornerRadius = 12.dp,
                            backgroundColor = if (selectedType == DeleteActionType.DELETE_FILE) Color(0x33FF4081) else Color(0x1AFFFFFF)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == DeleteActionType.DELETE_FILE,
                        onClick = { selectedType = DeleteActionType.DELETE_FILE },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF4081))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Borrar archivo original", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Elimina el archivo del almacenamiento", color = Color(0xFFFF80AB), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(selectedType) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == DeleteActionType.DELETE_FILE) Color(0xFFFF4081) else Color(0xFF00E5FF)
                )
            ) {
                Text(
                    text = if (selectedType == DeleteActionType.DELETE_FILE) "Borrar archivo" else "Quitar",
                    color = if (selectedType == DeleteActionType.DELETE_FILE) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}
