package com.example.laravelpos.ui.theme.login

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.laravelpos.viewmodel.LoginViewModel
import com.example.laravelpos.BuildConfig

@Composable
fun LoginScreen(navController: NavController) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    val (emailFocusRequester, passwordFocusRequester) = remember { FocusRequester.createRefs() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        ServerSettingsDialog(
            currentIp = viewModel.getServerIp(),
            onDismiss = { showSettingsDialog = false },
            onConfirm = { newIp ->
                viewModel.updateServerIp(newIp)
                showSettingsDialog = false
            }
        )
    }

    // Usamos un único Column scrollable para evitar que los elementos se superpongan
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Estado para la animación de la imagen
                var targetOffset by remember { mutableStateOf(0.dp) }
                val animatedOffset: Dp by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 1500),
                    label = "offsetAnimation"
                )

                // Este efecto se lanza una vez y luego se repite para la animación
                LaunchedEffect(Unit) {
                    while (true) {
                        targetOffset = 15.dp
                        kotlinx.coroutines.delay(1500)
                        targetOffset = 0.dp
                        kotlinx.coroutines.delay(1500)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Image(
                    painter = painterResource(id = com.example.laravelpos.R.drawable.pos_image),
                    contentDescription = "POS system image",
                    modifier = Modifier
                        .size(150.dp)
                        .offset(y = animatedOffset) // Aplicamos la animación
                        .padding(bottom = 32.dp)
                )

                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            // Al presionar 'Siguiente', movemos el foco al siguiente campo
                            passwordFocusRequester.requestFocus()
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            viewModel.login(email, password) {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.login(email, password) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                }

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // La versión ahora está DENTRO del scroll, asegurando que no tape nada
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Versión ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // Ruedita de ajustes en la esquina superior derecha
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configuración de servidor",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ServerSettingsDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var ipText by remember { mutableStateOf(currentIp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración del Servidor") },
        text = {
            Column {
                Text("Ingrese la dirección IP del servidor Laravel:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    label = { Text("Dirección IP") },
                    singleLine = true,
                    placeholder = { Text("Ej: 192.168.1.100") }
                )
                Text(
                    text = "* Se requiere reiniciar la aplicación para aplicar los cambios por completo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ipText) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
