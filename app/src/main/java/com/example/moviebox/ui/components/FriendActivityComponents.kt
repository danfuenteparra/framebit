package com.example.moviebox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Tarjeta para mostrar una peli/serie/juego que un amigo ha visto.
 * Muestra el póster con la foto del amigo en la esquina inferior izquierda.
 *
 * @param mediaType "movie" | "tv" | "game" — determina si la URL del poster
 *                  es de TMDB (necesita prefijo) o RAWG (URL directa) y la forma de la card.
 */
@Composable
fun FriendActivityCard(
    activity: FriendActivity,
    mediaType: String,
    onClick: () -> Unit
) {
    val isGame = mediaType == "game"
    val cardWidth = if (isGame) 160.dp else 130.dp
    val imageHeight = if (isGame) 100.dp else 180.dp
    val imageUrl = if (isGame) activity.posterPath
    else TmdbApiService.getImageUrl(activity.posterPath)

    Card(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = activity.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Avatar del amigo en la esquina inferior izquierda
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MovieBoxBackground),
                contentAlignment = Alignment.Center
            ) {
                if (activity.friendPicture != null) {
                    AsyncImage(
                        model = activity.friendPicture,
                        contentDescription = activity.friendName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MovieBoxOnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fila completa "De tus amigos" lista para usar.
 * Si la lista está vacía, no renderiza nada (el caller decide si mostrar título).
 */
@Composable
fun FriendsActivityRow(
    activities: List<FriendActivity>,
    mediaType: String,
    onItemClick: (mediaId: Int) -> Unit
) {
    if (activities.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(activities, key = { "${it.friendUserId}_${it.mediaId}_${it.watchedAt}" }) { act ->
            FriendActivityCard(
                activity = act,
                mediaType = mediaType,
                onClick = { onItemClick(act.mediaId) }
            )
        }
    }
}

/**
 * Mensaje placeholder a mostrar cuando no hay amigos o no han visto nada.
 */
@Composable
fun EmptyFriendsActivityHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MovieBoxOnBackground.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}