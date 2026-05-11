package com.example.framebit.ui.screens.userprofile.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.framebit.data.remote.api.TmdbApiService
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary

/**
 * Item plano para mostrar en las pantallas de sección del perfil ajeno.
 * Incluye [rating] cuando hay una reseña asociada (para pintar el badge).
 */
data class SectionMediaItem(
    val mediaType: String,
    val mediaId: Int,
    val title: String,
    val posterPath: String?,
    val rating: Float? = null
)

/**
 * Pantalla compartida por UserWatchedScreen / UserReviewsScreen / UserWatchlistScreen.
 *
 * Renderiza un Scaffold con TopAppBar y 3 secciones (Películas, Series, Juegos),
 * cada una con preview horizontal y botón "Ver todas" que abre UserMediaList.
 *
 * Cada sección se oculta entera si no hay items de ese tipo.
 *
 * @param title título mostrado en la TopAppBar.
 * @param movies / tvShows / games items a mostrar por sección.
 * @param previewLimit cuántos items mostrar en el preview horizontal (resto en "Ver todas").
 * @param onItemClick callback al pulsar un póster (mediaType, mediaId).
 * @param onSeeAll callback al pulsar "Ver todas" en una sección (mediaType).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSectionScreen(
    title: String,
    movies: List<SectionMediaItem>,
    tvShows: List<SectionMediaItem>,
    games: List<SectionMediaItem>,
    loading: Boolean,
    onBack: () -> Unit,
    onItemClick: (mediaType: String, mediaId: Int) -> Unit,
    onSeeAll: (mediaType: String) -> Unit,
    previewLimit: Int = 12
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
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

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MovieBoxPrimary)
            }
            return@Scaffold
        }

        // Si no hay nada en ninguna sección, mensaje vacío
        val allEmpty = movies.isEmpty() && tvShows.isEmpty() && games.isEmpty()
        if (allEmpty) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay nada por aquí.",
                    color = MovieBoxOnBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (movies.isNotEmpty()) {
                MediaSection(
                    label = "Películas",
                    mediaType = "movie",
                    items = movies,
                    previewLimit = previewLimit,
                    onItemClick = onItemClick,
                    onSeeAll = onSeeAll
                )
            }
            if (tvShows.isNotEmpty()) {
                MediaSection(
                    label = "Series",
                    mediaType = "tv",
                    items = tvShows,
                    previewLimit = previewLimit,
                    onItemClick = onItemClick,
                    onSeeAll = onSeeAll
                )
            }
            if (games.isNotEmpty()) {
                MediaSection(
                    label = "Juegos",
                    mediaType = "game",
                    items = games,
                    previewLimit = previewLimit,
                    onItemClick = onItemClick,
                    onSeeAll = onSeeAll
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Bloque de una sección: cabecera con etiqueta + total + "Ver todas",
 * y LazyRow horizontal con [previewLimit] pósters.
 */
@Composable
private fun MediaSection(
    label: String,
    mediaType: String,
    items: List<SectionMediaItem>,
    previewLimit: Int,
    onItemClick: (String, Int) -> Unit,
    onSeeAll: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabecera de sección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    color = MovieBoxPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "· ${items.size}",
                    color = MovieBoxOnBackground.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Solo mostramos "Ver todas" si hay más items que el preview
            if (items.size > previewLimit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSeeAll(mediaType) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Ver todas",
                        color = MovieBoxOnBackground.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MovieBoxOnBackground.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preview horizontal
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items.take(previewLimit),
                key = { "${it.mediaType}_${it.mediaId}" }
            ) { item ->
                MediaPosterCard(
                    item = item,
                    onClick = { onItemClick(item.mediaType, item.mediaId) }
                )
            }
        }
    }
}

/**
 * Tarjeta de póster con badge de nota opcional en la esquina superior derecha.
 * Tamaño fijo (100dp ancho) para el preview horizontal; en grid usar variante.
 */
@Composable
fun MediaPosterCard(
    item: SectionMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGame = item.mediaType == "game"
    val imageUrl = if (isGame) item.posterPath
    else TmdbApiService.getImageUrl(item.posterPath)

    Box(
        modifier = modifier
            .width(100.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Badge con la nota (si tiene reseña)
        if (item.rating != null) {
            RatingBadge(
                rating = item.rating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Pequeño badge oscuro semitransparente con estrella + nota (1 decimal).
 */
@Composable
fun RatingBadge(rating: Float, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MovieBoxPrimary,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = String.format("%.1f", rating),
                color = MovieBoxOnBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}