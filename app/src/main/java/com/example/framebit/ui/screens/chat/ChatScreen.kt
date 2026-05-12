package com.example.framebit.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.framebit.data.remote.api.TmdbApiService
import com.example.framebit.data.remote.model.ChatMessage
import com.example.framebit.data.remote.model.MessageAttachment
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onGameClick: (Int) -> Unit,
    onReviewClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val otherUser by viewModel.otherUser.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    var showMediaDialog by remember { mutableStateOf(false) }
    val myId = viewModel.myUserId

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun navigateToMedia(mediaType: String, mediaId: Int) {
        when (mediaType) {
            "movie" -> onMovieClick(mediaId)
            "tv" -> onTvShowClick(mediaId)
            "game" -> onGameClick(mediaId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable {
                            if (viewModel.otherUserId.isNotBlank()) onUserProfileClick(viewModel.otherUserId)
                        }
                    ) {
                        val pic = otherUser?.pictureUrl
                        if (pic != null) {
                            AsyncImage(
                                model = pic,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground)
                            }
                        }
                        Text(
                            text = otherUser?.name?.ifBlank { "Usuario" } ?: "Usuario",
                            color = MovieBoxOnBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground,
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón para adjuntar contenido
                FilledIconButton(
                    onClick = { showMediaDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MovieBoxSurface
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adjuntar contenido", tint = MovieBoxPrimary)
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Escribe un mensaje...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    ),
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            viewModel.sendText(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MovieBoxPrimary,
                        disabledContainerColor = MovieBoxSurface
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.messageId }) { msg ->
                MessageBubble(
                    message = msg,
                    isMine = msg.senderId == myId,
                    onMediaClick = ::navigateToMedia,
                    onReviewClick = onReviewClick
                )
            }
        }
    }

    if (showMediaDialog) {
        ChatMediaSearchDialog(
            viewModel = viewModel,
            onDismiss = {
                showMediaDialog = false
                viewModel.clearSearch()
            },
            onSent = {
                showMediaDialog = false
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onMediaClick: (String, Int) -> Unit,
    onReviewClick: (String) -> Unit
) {
    val bubbleColor = if (isMine) MovieBoxPrimary else MovieBoxSurface
    val textColor = if (isMine) Color.Black else MovieBoxOnBackground
    val align = if (isMine) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        when (message.type) {
            "media" -> {
                val a = message.attachment
                if (a != null) MediaAttachmentCard(a) { onMediaClick(a.mediaType, a.mediaId) }
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextBubble(message.text, bubbleColor, textColor)
                }
            }
            "review" -> {
                val a = message.attachment
                if (a != null && a.reviewId != null) ReviewAttachmentCard(a) { onReviewClick(a.reviewId) }
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextBubble(message.text, bubbleColor, textColor)
                }
            }
            else -> TextBubble(message.text, bubbleColor, textColor)
        }

        if (message.createdAt > 0L) {
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt)),
                color = MovieBoxOnBackground.copy(alpha = 0.45f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TextBubble(text: String, bubbleColor: Color, textColor: Color) {
    Surface(color = bubbleColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 280.dp)) {
        Text(text = text, color = textColor, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun MediaAttachmentCard(attachment: MessageAttachment, onClick: () -> Unit) {
    val imageUrl = if (attachment.mediaType == "game") attachment.posterPath
    else TmdbApiService.getImageUrl(attachment.posterPath)
    val typeLabel = when (attachment.mediaType) {
        "movie" -> "Pelicula"
        "tv" -> "Serie"
        "game" -> "Videojuego"
        else -> ""
    }

    Card(
        modifier = Modifier.widthIn(max = 260.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = attachment.title,
                modifier = Modifier.width(60.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.align(Alignment.CenterVertically).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(typeLabel, color = MovieBoxPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(attachment.title, color = MovieBoxOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (attachment.releaseYear.isNotBlank()) {
                    Text(attachment.releaseYear, color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReviewAttachmentCard(attachment: MessageAttachment, onClick: () -> Unit) {
    val imageUrl = if (attachment.mediaType == "game") attachment.posterPath
    else TmdbApiService.getImageUrl(attachment.posterPath)

    Card(
        modifier = Modifier.widthIn(max = 260.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = attachment.title,
                modifier = Modifier.width(60.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.align(Alignment.CenterVertically).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Resena", color = MovieBoxPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(attachment.title, color = MovieBoxOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (attachment.reviewAuthorName != null) {
                    Text("Por ${attachment.reviewAuthorName}", color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                if (attachment.reviewRating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i < attachment.reviewRating) MovieBoxPrimary
                                else MovieBoxOnBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}