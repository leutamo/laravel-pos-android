package com.example.laravelpos.ui.theme.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.laravelpos.data.model.Product
import com.example.laravelpos.viewmodel.HomeViewModel

private const val TAG = "CartScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val context = LocalContext.current // Obtenemos el contexto actual

    val cartItems by homeViewModel.cartItems.collectAsState()
    val totalAmount by homeViewModel.totalAmount.collectAsState()
    val igvAmount by homeViewModel.igvAmount.collectAsState()
    // Modal values
    val selectedReceiptType by homeViewModel.selectedReceiptType.collectAsState()
    val showReceiptModal by homeViewModel.showReceiptModal.collectAsState()
    // --- LÍNEA DE DEPURACIÓN AÑADIDA ---
    Toast.makeText(context, "CartScreen: cartItems size is ${cartItems.size}", Toast.LENGTH_SHORT).show()
    // ---------------------------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Tu Carrito") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sección superior: Total y subtotal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Total: S/ ${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "IGV: S/ ${String.format("%.2f", igvAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de productos en el carrito
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(cartItems) { product ->
                    CartItemCard(product, homeViewModel)
                }
            }

            // Separador
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.LightGray)
            )

            // Sección inferior: Botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { homeViewModel.clearCart() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Vaciar Carrito", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { homeViewModel.showReceiptModal() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Siguiente", color = Color.White)
                }
            }
        }

        // Modal para seleccionar el tipo de comprobante
        if (showReceiptModal) {
            ModalBottomSheet(
                onDismissRequest = { homeViewModel.hideReceiptModal() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Selecciona tipo de comprobante",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { homeViewModel.selectReceiptType("Nota de Venta") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nota de Venta")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { homeViewModel.selectReceiptType("Boleta de Venta") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Boleta de Venta")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { homeViewModel.selectReceiptType("Factura") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Factura")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { homeViewModel.hideReceiptModal() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.error, MaterialTheme.shapes.medium),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }

        // Navegación a Checkout tras seleccionar (evitar bucles)
        LaunchedEffect(selectedReceiptType) {
            if (selectedReceiptType != null && navController.currentBackStackEntry?.destination?.route != "checkout") {
                navController.navigate("checkout") {
                    launchSingleTop = true
                    popUpTo("cart") { inclusive = false }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(product: Product, homeViewModel: HomeViewModel) {
    // Observamos el StateFlow de las cantidades para forzar la recomposición.
    val itemQuantities by homeViewModel.itemQuantities.collectAsState()

    // Convertimos el ID del producto a String para que coincida con la clave del mapa.
    val quantity = itemQuantities[product.id.toString()] ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto
            AsyncImage(
                model = product.attributes.images.firstOrNull() ?: "",
                contentDescription = product.attributes.name,
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Detalles del producto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.attributes.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Precio: S/ ${product.attributes.product_price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "Stock: ${product.attributes.stock?.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Total: S/ ${String.format("%.2f", homeViewModel.calculateItemTotal(product))}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Cantidad y botones
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { homeViewModel.incrementProduct(product) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir")
                }
                Text(
                    //text = "${homeViewModel.getProductCount(product)}",
                    text = "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { homeViewModel.decrementProduct(product) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Quitar")
                }
            }
        }
    }
}