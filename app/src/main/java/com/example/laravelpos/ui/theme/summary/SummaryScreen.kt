package com.example.laravelpos.ui.theme.summary

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.laravelpos.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(navController: NavController, homeViewModel: HomeViewModel) {
    val cartItems by homeViewModel.cartItems.collectAsState()
    val totalAmount by homeViewModel.totalAmount.collectAsState()
    val igvAmount by homeViewModel.igvAmount.collectAsState()
    val itemQuantities by homeViewModel.itemQuantities.collectAsState()
    val selectedReceiptType by homeViewModel.selectedReceiptType.collectAsState()

    var emailText by remember { mutableStateOf("") }

    // Valores fijos para el resumen, basados en el diseño
    val subTotal = totalAmount / 1.18
    val totalPaid = 0.50
    val change = 0.10

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedReceiptType ?: "Resumen de Compra",
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                },
                /*                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Regresar",
                                            tint = Color.White
                                        )
                                    }
                                },*/
                actions = {
                    IconButton(onClick = { /* TODO: Lógica de compartir */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Título de totales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Total cobrado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "S/ ${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Encabezados de la tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRODUCTO",
                    modifier = Modifier.weight(2f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PRECIO",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "#",
                    modifier = Modifier.weight(0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "UNIDAD",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "TOTAL",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Lista de productos
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(cartItems) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.attributes.name,
                            modifier = Modifier.weight(2f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "S/ ${String.format("%.2f", product.attributes.product_price)}",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${itemQuantities[product.id.toString()] ?: 0}",
                            modifier = Modifier.weight(0.5f)
                        )
                        Text(
                            text = "UNIDA",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "S/ ${
                                String.format(
                                    "%.2f",
                                    homeViewModel.calculateItemTotal(product)
                                )
                            }",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sección de totales y vuelto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Sub Total", modifier = Modifier.weight(1f))
                    Text(
                        text = "S/ ${String.format("%.2f", subTotal)}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "IGV", modifier = Modifier.weight(1f))
                    Text(
                        text = "S/ ${String.format("%.2f", igvAmount)}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TOTAL",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "S/ ${String.format("%.2f", totalAmount)}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Pago:", modifier = Modifier.weight(1f))
                    Text(
                        text = "S/ ${String.format("%.2f", totalPaid)}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Vuelto:", modifier = Modifier.weight(1f))
                    Text(text = "S/ ${String.format("%.2f", change)}", fontWeight = FontWeight.Bold)
                }
            }

            // Sección de Email y botones de acción
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { /* TODO: Lógica de envío */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("Enviar")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            // Borra toda la pila y navega a la pantalla de inicio
                            navController.popBackStack("home", inclusive = false)
                            homeViewModel.clearCart() // Limpia el carrito para la próxima venta
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Finalizar")
                    }
                }
            }
        }
    }
}