package com.example.framebit.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.framebit.data.remote.api.TmdbApiService
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMediaSearchDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onSent: () -> Unit
) {
    val searchType by viewModel.searchType.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val state by viewModel.searchState.collectAsStateWithLifecycle()

    fun handleSelect(mediaType: String, id: Int, title: String, poster: String?, year: String) {
        viewModel.sendMedia(mediaType, id, title, poster, year)
        viewModel.clearSearch()
        onSent()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MovieBoxBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Enviar contenido", color = MovieBoxOnBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MovieBoxOnBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
                )

                // Tabs tipo / búsqueda
                TabRow(
                    selectedTabIndex = searchType.ordinal,
                    containerColor = MovieBoxSurface,
                    contentColor = MovieBoxOnBackground
                ) {
                    Tab(
                        selected = searchType == ChatSearchType.MOVIE,
                        onClick = { viewModel.setSearchType(ChatSearchType.MOVIE) },
                        text = { Text("Pelis", color = if (searchType == ChatSearchType.MOVIE) MovieBoxPrimary else MovieBoxOnBackground) }
                    )
                    Tab(
                        selected = searchType == ChatSearchType.TV,
                        onClick = { viewModel.setSearchType(ChatSearchType.TV) },
                        text = { Text("Series", color = if (searchType == ChatSearchType.TV) MovieBoxPrimary else MovieBoxOnBackground) }
                    )
                    Tab(
                        selected = searchType == ChatSearchType.GAME,
                        onClick = { viewModel.setSearchType(ChatSearchType.GAME) },
                        text = { Text("Juegos", color = if (searchType == ChatSearchType.GAME) MovieBoxPrimary else MovieBoxOnBackground) }
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Buscar...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MovieBoxOnBackground) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )

                when (state) {
                    is ChatSearchState.Idle -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Escribe para buscar...", color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                    is ChatSearchState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MovieBoxPrimary)
                    }
                    is ChatSearchState.Movies -> {
                        val items = (state as ChatSearchState.Movies).items
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items) { m ->
                                ResultRow(
                                    title = m.title,
                                    subtitle = m.releaseDate.take(4),
                                    poster = TmdbApiService.getImageUrl(m.posterPath),
                                    onClick = { handleSelect("movie", m.id, m.title, m.posterPath, m.releaseDate.take(4)) }
                                )
                            }
                        }
                    }
                    is ChatSearchState.TvShows -> {
                        val items = (state as ChatSearchState.TvShows).items
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items) { t ->
                                ResultRow(
                                    title = t.name,
                                    subtitle = t.firstAirDate.take(4),
                                    poster = TmdbApiService.getImageUrl(t.posterPath),
                                    onClick = { handleSelect("tv", t.id, t.name, t.posterPath, t.firstAirDate.take(4)) }
                                )
                            }
                        }
                    }
                    is ChatSearchState.Games -> {
                        val items = (state as ChatSearchState.Games).items
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items) { g ->
                                ResultRow(
                                    title = g.name,
                                    subtitle = g.released?.take(4) ?: "",
                                    poster = g.backgroundImage,
                                    onClick = { handleSelect("game", g.id, g.name, g.backgroundImage, g.released?.take(4) ?: "") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, poster: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = poster,
                contentDescription = title,
                modifier = Modifier.size(width = 40.dp, height = 56.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MovieBoxOnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = MovieBoxOnBackground.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}