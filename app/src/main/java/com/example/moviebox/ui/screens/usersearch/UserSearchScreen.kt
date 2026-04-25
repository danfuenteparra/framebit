package com.example.moviebox.ui.screens.usersearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar usuarios", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Nombre del usuario...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MovieBoxOnBackground) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MovieBoxOnBackground,
                    unfocusedTextColor = MovieBoxOnBackground,
                    focusedBorderColor = MovieBoxPrimary,
                    unfocusedBorderColor = MovieBoxSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MovieBoxPrimary)
                    }
                }
                query.isBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Escribe un nombre para buscar",
                            color = MovieBoxOnBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
                results.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Sin resultados",
                            color = MovieBoxOnBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { it.userId }) { user ->
                            UserListRow(
                                name = user.name,
                                pictureUrl = user.pictureUrl,
                                onClick = { onUserClick(user.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListRow(
    name: String,
    pictureUrl: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pictureUrl != null) {
                AsyncImage(
                    model = pictureUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MovieBoxOnBackground)
                }
            }
            Text(
                text = name,
                color = MovieBoxOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
