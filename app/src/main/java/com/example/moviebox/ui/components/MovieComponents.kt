package com.example.moviebox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Card de película reutilizable
 * Se usa en Home, Search y cualquier lista de películas
 */
@Composable
fun MovieCard(
    title: String,
    posterPath: String?,
    voteAverage: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        Column {
            AsyncImage(
                model = TmdbApiService.getImageUrl(posterPath),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                color = MovieBoxOnBackground,
                fontSize = 12.sp,
                maxLines = 2,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = "⭐ ${String.format("%.1f", voteAverage)}",
                color = MovieBoxPrimary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Título de sección reutilizable
 * Se usa para separar secciones en listas
 */
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MovieBoxOnBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Indicador de rating reutilizable
 */
@Composable
fun RatingText(voteAverage: Double) {
    Text(
        text = "⭐ ${String.format("%.1f", voteAverage)}",
        color = MovieBoxPrimary,
        fontSize = 12.sp
    )
}