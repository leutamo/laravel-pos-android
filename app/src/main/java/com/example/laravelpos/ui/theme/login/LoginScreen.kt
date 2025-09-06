package com.example.laravelpos.ui.theme.login

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

    // Usamos un Box para superponer y posicionar elementos
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Columna para el contenido principal, centrada en el Box
        Column(
            modifier = Modifier
                .align(Alignment.Center).padding(start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
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
                CircularProgressIndicator()
            }

            state.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }

        // El texto de la versión se alinea al fondo del Box
        Text(
            text = "Versión ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}
