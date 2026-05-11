package com.example.moviebox.ui.screens.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.model.ChatThread
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onChatClick: (chatId: String, otherUserId: String) -> Unit,
    onNewChat: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val myUserId = viewModel.myUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras", tint = MovieBoxOnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Default.Edit, contentDescription = "Nuevo chat", tint = MovieBoxPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tienes mensajes", color = MovieBoxOnBackground.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Pulsa el icono de arriba para iniciar un chat con alguien con quien os seguis mutuamente.",
                        color = MovieBoxOnBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(chats, key = { it.chatId }) { chat ->
                ChatRow(
                    chat = chat,
                    myUserId = myUserId,
                    onClick = { onChatClick(chat.chatId, chat.otherUserId(myUserId)) }
                )
            }
        }
    }
}

@Composable
private fun ChatRow(
    chat: ChatThread,
    myUserId: String,
    onClick: () -> Unit
) {
    val otherName = chat.otherUserName(myUserId).ifBlank { "Usuario" }
    val otherPicture = chat.otherUserPicture(myUserId)
    val unread = chat.unreadFor(myUserId)
    val isUnread = unread > 0
    val timeText = formatTime(chat.lastMessageAt)

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (otherPicture != null) {
            AsyncImage(
                model = otherPicture,
                contentDescription = otherName,
                modifier = Modifier.size(52.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground, modifier = Modifier.size(28.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = otherName,
                color = MovieBoxOnBackground,
                fontSize = 15.sp,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.lastMessage.ifBlank { "Sin mensajes" },
                color = if (isUnread) MovieBoxOnBackground else MovieBoxOnBackground.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    color = if (isUnread) MovieBoxPrimary else MovieBoxOnBackground.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (isUnread) {
                Surface(color = MovieBoxPrimary, shape = CircleShape, modifier = Modifier.size(20.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (unread > 99) "99+" else unread.toString(),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MovieBoxSurface.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 80.dp))
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = Calendar.getInstance()
    val msg = Calendar.getInstance().apply { timeInMillis = timestamp }
    return if (now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
    ) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    else SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
}