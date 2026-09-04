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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.laravelpos.viewmodel.HomeViewModel
import com.example.laravelpos.viewmodel.SummaryViewModel
import com.example.laravelpos.viewmodel.CheckoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    type: String,
    id: Int,
    summaryViewModel: SummaryViewModel = hiltViewModel(),
    checkoutViewModel: CheckoutViewModel = hiltViewModel()
) {
    val state by summaryViewModel.state.collectAsState()
    val quotation = state.quotation

    LaunchedEffect(id, type) {
        summaryViewModel.loadData(type, id)
    }

    var emailText by remember { mutableStateOf("") }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else if (quotation != null) {
        val attr = quotation.attributes
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val titleText = if (type == "sale") "Venta" else "Cotización"
                            Text(
                                text = "$titleText #${attr.referenceCode}",
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    },
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
                        text = "S/ ${String.format("%.2f", attr.grandTotal)}",
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
                    Text(text = "PRODUCTO", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text(text = "PRECIO", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(text = "#", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
                    Text(text = "UNIDAD", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(text = "TOTAL", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Lista de productos
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(attr.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Producto ${item.productId}",
                                modifier = Modifier.weight(2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = "S/ ${String.format("%.2f", item.productPrice)}", modifier = Modifier.weight(1f))
                            Text(text = "${item.quantity}", modifier = Modifier.weight(0.5f))
                            Text(text = item.saleUnit.shortName, modifier = Modifier.weight(1f))
                            Text(text = "S/ ${String.format("%.2f", item.subTotal)}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
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
                        Text(text = "S/ ${String.format("%.2f", attr.grandTotal - attr.taxAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "IGV (${attr.taxRate}%)", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.taxAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "TOTAL", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(text = "S/ ${String.format("%.2f", attr.grandTotal)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Pago:", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.receivedAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Vuelto:", modifier = Modifier.weight(1f))
                        Text(text = "S/ ${String.format("%.2f", attr.receivedAmount - attr.grandTotal)}", fontWeight = FontWeight.Bold)
                    }
                }

                // Sección de Email y botones de acción
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = emailText,
                        onValueChange = { emailText = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                                checkoutViewModel.clearCheckoutData() // ✅ Limpiar datos del cliente anterior
                                homeViewModel.clearCart() // ✅ Por si acaso
                                homeViewModel.selectReceiptType(null)
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
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
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = state.error ?: "Error desconocido")
        }
    }
}