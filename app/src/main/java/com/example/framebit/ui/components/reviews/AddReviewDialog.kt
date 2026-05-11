package com.example.framebit.ui.components.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

@Composable
fun AddReviewDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float, String) -> Unit
) {
    var rating by remember { mutableFloatStateOf(3f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MovieBoxSurface,
        title = { Text("Escribir reseña", color = MovieBoxOnBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Puntuación", color = MovieBoxOnBackground, fontSize = 14.sp)
                Row {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = star.toFloat() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$star estrellas",
                                tint = if (star <= rating) MovieBoxPrimary
                                else MovieBoxOnBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Tu opinión") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxOnBackground.copy(alpha = 0.3f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (comment.isNotBlank()) onConfirm(rating, comment) },
                enabled = comment.isNotBlank()
            ) {
                Text("Guardar", color = MovieBoxPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MovieBoxOnBackground)
            }
        }
    )
}
