package com.example.framebit.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.framebit.ui.theme.MovieBoxBackground
import com.example.framebit.ui.theme.MovieBoxOnBackground
import com.example.framebit.ui.theme.MovieBoxPrimary
import com.example.framebit.ui.theme.MovieBoxSurface

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tabIndex by remember { mutableIntStateOf(0) }
    var showEmailForm by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
    }

    val isLoading = uiState is LoginUiState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MovieBoxBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Framebit",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MovieBoxPrimary
            )
            Text(
                text = "Gestiona películas, series y videojuegos",
                fontSize = 16.sp,
                color = MovieBoxOnBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!showEmailForm) {
                Button(
                    onClick = { viewModel.login(context) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MovieBoxPrimary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Iniciar sesión con Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        showEmailForm = true
                        viewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Email y contraseña",
                        color = MovieBoxOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Tabs Entrar / Registrarse
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = MovieBoxSurface,
                    contentColor = MovieBoxOnBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0; viewModel.clearError() },
                        text = {
                            Text(
                                "Entrar",
                                color = if (tabIndex == 0) MovieBoxPrimary else MovieBoxOnBackground
                            )
                        }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1; viewModel.clearError() },
                        text = {
                            Text(
                                "Registrarse",
                                color = if (tabIndex == 1) MovieBoxPrimary else MovieBoxOnBackground
                            )
                        }
                    )
                }

                if (tabIndex == 1) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nombre", color = MovieBoxOnBackground.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MovieBoxOnBackground,
                            unfocusedTextColor = MovieBoxOnBackground,
                            focusedBorderColor = MovieBoxPrimary,
                            unfocusedBorderColor = MovieBoxSurface
                        )
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = MovieBoxOnBackground.copy(alpha = 0.7f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = MovieBoxOnBackground.copy(alpha = 0.7f)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                tint = MovieBoxOnBackground
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MovieBoxOnBackground,
                        unfocusedTextColor = MovieBoxOnBackground,
                        focusedBorderColor = MovieBoxPrimary,
                        unfocusedBorderColor = MovieBoxSurface
                    )
                )

                Button(
                    onClick = {
                        if (tabIndex == 0) viewModel.loginEmail(email, password)
                        else viewModel.registerEmail(email, password, displayName)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MovieBoxPrimary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (tabIndex == 0) "Entrar" else "Crear cuenta",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = {
                        showEmailForm = false
                        viewModel.clearError()
                    },
                    enabled = !isLoading
                ) {
                    Text("← Volver", color = MovieBoxOnBackground)
                }
            }

            if (uiState is LoginUiState.Error) {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }
    }
}