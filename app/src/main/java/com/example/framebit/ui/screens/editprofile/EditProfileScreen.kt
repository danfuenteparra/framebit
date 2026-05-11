package com.example.framebit.ui.screens.editprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val links by viewModel.links.collectAsStateWithLifecycle()
    val pictureUrl by viewModel.pictureUrl.collectAsStateWithLifecycle()
    val pendingUri by viewModel.pendingPictureUri.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) onSaved()
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) viewModel.pickPicture(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil", color = MovieBoxOnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MovieBoxOnBackground)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !saving
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MovieBoxPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Guardar", color = MovieBoxPrimary, fontWeight = FontWeight.Bold)
                        }
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
            ) { CircularProgressIndicator(color = MovieBoxPrimary) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.size(120.dp)) {
                val displayModel: Any? = pendingUri ?: pictureUrl
                if (displayModel != null) {
                    AsyncImage(
                        model = displayModel,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MovieBoxOnBackground,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
                FloatingActionButton(
                    onClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp),
                    containerColor = MovieBoxPrimary
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto", tint = MovieBoxBackground)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Toca la foto para cambiarla",
                fontSize = 12.sp,
                color = MovieBoxOnBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bio
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Bio", color = MovieBoxOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { viewModel.updateBio(it) },
                    placeholder = { Text("Cuéntanos algo sobre ti...", color = MovieBoxOnBackground.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    ),
                    supportingText = {
                        Text(
                            "${bio.length}/280",
                            color = MovieBoxOnBackground.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enlaces
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enlaces", color = MovieBoxOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                TextButton(
                    onClick = { viewModel.addLink() },
                    enabled = links.size < 5
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MovieBoxPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir", color = MovieBoxPrimary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            links.forEachIndexed { index, link ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { viewModel.updateLink(index, it) },
                        placeholder = { Text("instagram.com/usuario", color = MovieBoxOnBackground.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = MovieBoxOnBackground) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MovieBoxOnBackground,
                            unfocusedTextColor = MovieBoxOnBackground,
                            focusedBorderColor = MovieBoxPrimary,
                            unfocusedBorderColor = MovieBoxSurface
                        )
                    )
                    IconButton(onClick = { viewModel.removeLink(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar", tint = MovieBoxOnBackground.copy(alpha = 0.7f))
                    }
                }
            }

            if (links.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Añade hasta 5 enlaces (redes sociales, web personal...)",
                    fontSize = 12.sp,
                    color = MovieBoxOnBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Mostrar error
            error?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.consumeError()
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}