package com.example.framebit.ui.screens.sharetochat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

/**
 * Diálogo para compartir un contenido (peli/serie/juego/reseña) a un amigo.
 * Carga la lista de mutual follows al abrirse y permite escribir un caption opcional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatDialog(
    target: ShareTarget,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
    viewModel: ShareToChatViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var caption by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadCandidates() }
    LaunchedEffect(state) {
        if (state is ShareUiState.Sent) {
            onSent()
            viewModel.reset()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MovieBoxBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Enviar \"${target.title}\"",
                            color = MovieBoxOnBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MovieBoxOnBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Mensaje (opcional)...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    ),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Text(
                    text = "Elige a quien enviar",
                    color = MovieBoxOnBackground.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                when (state) {
                    is ShareUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MovieBoxPrimary)
                    }
                    is ShareUiState.Error -> Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (state as ShareUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                    else -> {
                        if (users.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No tienes seguimientos mutuos a los que enviar.",
                                    color = MovieBoxOnBackground.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(users, key = { it.userId }) { user ->
                                    val selected = user.userId == selectedUserId
                                    Surface(
                                        color = if (selected) MovieBoxPrimary.copy(alpha = 0.15f) else MovieBoxBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedUserId = user.userId }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (user.pictureUrl != null) {
                                                AsyncImage(
                                                    model = user.pictureUrl,
                                                    contentDescription = user.name,
                                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(modifier = Modifier.size(40.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground)
                                                }
                                            }
                                            Text(
                                                user.name,
                                                color = MovieBoxOnBackground,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (selected) {
                                                Surface(
                                                    color = MovieBoxPrimary,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(10.dp)
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Botón enviar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val u = selectedUserId
                            if (u != null) viewModel.send(target, u, caption.trim())
                        },
                        enabled = selectedUserId != null && state !is ShareUiState.Sending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MovieBoxPrimary,
                            disabledContainerColor = MovieBoxSurface
                        )
                    ) {
                        if (state is ShareUiState.Sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MovieBoxBackground
                            )
                        } else {
                            Text("Enviar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}