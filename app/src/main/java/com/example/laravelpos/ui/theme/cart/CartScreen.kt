package com.example.laravelpos.ui.theme.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.laravelpos.data.model.CartItem
import com.example.laravelpos.data.model.ProductConversion
import com.example.laravelpos.viewmodel.HomeViewModel

private const val TAG = "CartScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val cartItems by homeViewModel.cartItems.collectAsState()
    val totalAmount by homeViewModel.totalAmount.collectAsState()
    val igvAmount by homeViewModel.igvAmount.collectAsState()
    // Modal values
    val selectedReceiptType by homeViewModel.selectedReceiptType.collectAsState()
    val showReceiptModal by homeViewModel.showReceiptModal.collectAsState()

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
                items(cartItems) { cartItem ->
                    CartItemCard(cartItem, homeViewModel)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartItemCard(cartItem: CartItem, homeViewModel: HomeViewModel) {
    val product = cartItem.product
    val swipeToDismissState = rememberSwipeToDismissBoxState()
    
    var showUnitDialog by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }

    if (showUnitDialog) {
        UnitSelectionDialog(
            cartItem = cartItem,
            onDismiss = { showUnitDialog = false },
            onUnitSelected = { conversion ->
                homeViewModel.changeItemUnit(cartItem, conversion)
                showUnitDialog = false
            }
        )
    }

    if (showPriceDialog) {
        PriceEditDialog(
            currentPrice = cartItem.unitPrice,
            onDismiss = { showPriceDialog = false },
            onPriceConfirmed = { newPrice ->
                homeViewModel.changeItemPrice(cartItem, newPrice)
                showPriceDialog = false
            }
        )
    }

    // Efecto para detectar el swipe a la derecha (StartToEnd)
    LaunchedEffect(swipeToDismissState.currentValue) {
        if (swipeToDismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            showUnitDialog = true
            swipeToDismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissState,
        enableDismissFromEndToStart = false, // Desactivar swipe a la izquierda (borrar)
        backgroundContent = {
            val color = when (swipeToDismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2196F3) // Azul para cambio de unidad
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(color, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add, 
                        contentDescription = "Cambiar Unidad",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar Unidad", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
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
                    model = homeViewModel.getFullImageUrl(product.attributes.images?.imageUrls?.firstOrNull()),
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
                        text = "Unidad: ${cartItem.unitName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Precio Unit: S/ ${String.format("%.2f", cartItem.unitPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        IconButton(
                            onClick = { showPriceDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Precio",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = "Total: S/ ${String.format("%.2f", cartItem.subTotal)}",
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
                    IconButton(onClick = { homeViewModel.incrementProduct(cartItem) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir")
                    }
                    Text(
                        text = "${cartItem.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { homeViewModel.decrementProduct(cartItem) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Quitar")
                    }
                }
            }
        }
    }
}

@Composable
fun UnitSelectionDialog(
    cartItem: CartItem,
    onDismiss: () -> Unit,
    onUnitSelected: (ProductConversion?) -> Unit
) {
    val product = cartItem.product
    val conversions = product.attributes.conversions

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Unidad") },
        text = {
            Column {
                // Unidad base
                val isBaseSelected = cartItem.selectedConversion == null
                Surface(
                    onClick = { onUnitSelected(null) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isBaseSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(product.attributes.sale_unit_name.name)
                        Text("S/ ${String.format("%.2f", product.attributes.product_price)}")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Conversiones
                conversions.forEach { conversion ->
                    val isSelected = cartItem.selectedConversion?.id == conversion.id
                    Surface(
                        onClick = { onUnitSelected(conversion) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(conversion.toUnitName)
                            Text("S/ ${String.format("%.2f", conversion.price)}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PriceEditDialog(
    currentPrice: Double,
    onDismiss: () -> Unit,
    onPriceConfirmed: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(String.format("%.2f", currentPrice)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Precio Unitario") },
        text = {
            Column {
                Text("Ingrese el nuevo precio para este producto:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { 
                        // Solo permitir números y un punto decimal
                        if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") {
                            priceText = it 
                        }
                    },
                    label = { Text("Precio (S/)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPrice = priceText.toDoubleOrNull()
                    if (newPrice != null && newPrice >= 0) {
                        onPriceConfirmed(newPrice)
                    }
                }
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
