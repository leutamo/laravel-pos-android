package com.example.laravelpos.ui.theme.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.laravelpos.viewmodel.HomeViewModel
import com.example.laravelpos.viewmodel.LoginViewModel

// Se ha movido la constante TAG a nivel de archivo para evitar errores
private const val TAG = "HomeScreen"

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: LoginViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val userName by viewModel.userName.collectAsState()
    val filteredProducts by homeViewModel.filteredProducts.collectAsState()

    // Para actualizar el contador de carrito
    val cartItems by homeViewModel.cartItems.collectAsState()
    val cartItemCount = cartItems.size

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current // Obtenemos el contexto actual

    // Redirect to login if not authenticated
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Use WindowInsets.systemBars to handle padding for system bars
    val systemBarPadding =
        with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.systemBars.asPaddingValues())) {
        // Search bar and cart Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    homeViewModel.onSearchQueryChanged(it.text)
                },
                label = { Text("Buscar producto...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )
            // Shopping cart icon and counter
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 8.dp)
                    .clickable {
                        // TODO: Navigate to cart view
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Carrito de compras",
                    modifier = Modifier.size(28.dp)
                )
                // Counter overlay
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.Red, shape = MaterialTheme.shapes.small)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = cartItemCount.toString(), // Placeholder for cart count, se agrega contador de cartItem
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Product grid with 2 columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Log.d(TAG, "Card clicked for: ${product.attributes.name}")
                            homeViewModel.addItemToCart(product)
                            Toast.makeText(context, "Producto Agregado: ${product.attributes.name}", Toast.LENGTH_SHORT).show()
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // Top part: Image and product details
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product image
                            AsyncImage(
                                model = product.attributes.images.firstOrNull() ?: "",
                                contentDescription = "Imagen de ${product.attributes.name}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(end = 8.dp)
                            )

                            // Product name, stock, and code
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = product.attributes.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stock: ${product.attributes.stock?.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = product.attributes.product_code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Separator line
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.LightGray)
                        )

                        // Bottom part: Unit and price
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UNIDAD", // Assuming a fixed text for "Unidad"
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "S/ ${product.attributes.product_price}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Bottom section (logout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userName != null) {
                Text("Welcome, $userName!", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Welcome!", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            ) {
                Text("Logout", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// Preview function for HomeScreen
@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    // You'll need to provide mock data for the preview
    // as it doesn't have access to the ViewModel or NavController
    // This is a simplified example to show the layout
    Column(modifier = Modifier.fillMaxSize()) {

        OutlinedTextField(
            value = "Buscar producto...",
            onValueChange = {},
            label = { Text("Buscar producto...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(10) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(end = 8.dp)
                                    .background(Color.LightGray)
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Nombre del Producto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stock: 999",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "CÓDIGO123",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.LightGray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UNIDAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "S/ 10.00",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


