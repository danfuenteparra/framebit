package com.example.framebit.ui.screens.blockedusers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.framebit.data.remote.model.PublicUser
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

/**
 * Pantalla "Usuarios bloqueados" del perfil propio.
 *
 * Muestra la lista de usuarios que has bloqueado. Cada fila tiene:
 *   - Avatar + nombre (clickable: abre el perfil del usuario, que mostrará
 *     la tarjeta "Has bloqueado a este usuario" + botón desbloquear).
 *   - Botón "Desbloquear" inline para hacerlo sin entrar al perfil.
 *
 * Si la lista está vacía, mensaje claro de que no hay bloqueados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // Diálogo de confirmación al pulsar "Desbloquear"
    var pendingUnblock by remember { mutableStateOf<PublicUser?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Usuarios bloqueados", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MovieBoxOnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MovieBoxPrimary)
            }
            users.isEmpty() -> EmptyState(innerPadding)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = users, key = { it.userId }) { user ->
                        BlockedUserRow(
                            user = user,
                            onUserClick = { onUserClick(user.userId) },
                            onUnblockClick = { pendingUnblock = user }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de desbloqueo
    val pending = pendingUnblock
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingUnblock = null },
            containerColor = MovieBoxSurface,
            title = {
                Text("Desbloquear a ${pending.name}", color = MovieBoxOnBackground)
            },
            text = {
                Text(
                    "Si lo desbloqueas, podréis volver a ver vuestra actividad. " +
                            "No se restauran los follows previos: si quieres seguirle de nuevo, " +
                            "tendrás que hacerlo manualmente.",
                    color = MovieBoxOnBackground.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unblock(pending.userId)
                        pendingUnblock = null
                    }
                ) {
                    Text("Desbloquear", color = MovieBoxPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnblock = null }) {
                    Text("Cancelar", color = MovieBoxOnBackground)
                }
            }
        )
    }
}

/** Fila de usuario bloqueado: avatar, nombre y botón "Desbloquear". */
@Composable
private fun BlockedUserRow(
    user: PublicUser,
    onUserClick: () -> Unit,
    onUnblockClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MovieBoxSurface),
            contentAlignment = Alignment.Center
        ) {
            if (user.pictureUrl != null) {
                AsyncImage(
                    model = user.pictureUrl,
                    contentDescription = user.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MovieBoxOnBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        // Nombre y bio (resumen)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifBlank { "Usuario" },
                color = MovieBoxOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    color = MovieBoxOnBackground.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))

        // Botón "Desbloquear" (no consume click de la fila porque tiene su
        // propio onClick).
        Button(
            onClick = onUnblockClick,
            colors = ButtonDefaults.buttonColors(containerColor = MovieBoxPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Desbloquear", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Estado vacío cuando no hay usuarios bloqueados. */
@Composable
private fun EmptyState(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Block,
            contentDescription = null,
            tint = MovieBoxOnBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No tienes a ningún usuario bloqueado.",
            color = MovieBoxOnBackground.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}