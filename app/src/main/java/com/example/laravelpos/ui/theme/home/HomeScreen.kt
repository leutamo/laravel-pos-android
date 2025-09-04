package com.example.laravelpos.ui.theme.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.laravelpos.viewmodel.HomeViewModel
import com.example.laravelpos.viewmodel.LoginViewModel
import androidx.compose.runtime.LaunchedEffect // Añadir esta importación

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: LoginViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val userName by viewModel.userName.collectAsState()
    val filteredProducts by homeViewModel.filteredProducts.collectAsState()

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var searchText by remember { mutableStateOf(TextFieldValue("")) }

    // Redirigir al login si no está autenticado
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Buscador
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                homeViewModel.onSearchQueryChanged(it.text)
            },
            label = { Text("Buscar producto...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Lista de productos como tarjetas
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(filteredProducts) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Imagen del producto
                        AsyncImage(
                            model = product.attributes.images.firstOrNull() ?: "",
                            contentDescription = "Imagen de ${product.attributes.name}",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(end = 16.dp)
                        )

                        // Detalles del producto
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = product.attributes.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "S/ ${product.attributes.product_price}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Botón Añadir al Carrito
                        Button(
                            onClick = {
                                // Lógica temporal: muestra un mensaje
                                println("Añadiendo ${product.attributes.name} al carrito")
                            }
                        ) {
                            Text("Añadir")
                        }
                    }
                }
            }
        }

        // Sección inferior (logout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userName != null) {
                Text("Welcome to Home, $userName!")
            } else {
                Text("Welcome to Home!")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            ) {
                Text("Logout")
            }
        }
    }
}