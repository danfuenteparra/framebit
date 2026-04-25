package com.example.moviebox.ui.screens.userlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moviebox.ui.screens.usersearch.UserListRow
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: UserListViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.title, color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MovieBoxPrimary)
                    }
                }
                users.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay usuarios",
                            color = MovieBoxOnBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users, key = { it.userId }) { user ->
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
