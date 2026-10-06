package com.example.laravelpos.ui.theme.summary

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.laravelpos.viewmodel.CheckoutViewModel
import com.example.laravelpos.viewmodel.HomeViewModel
import com.example.laravelpos.viewmodel.SummaryViewModel
import java.net.URLEncoder

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
    val customerData by checkoutViewModel.customerData.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(id, type) {
        summaryViewModel.loadData(type, id)
    }

    var phoneText by remember { mutableStateOf("") }

    // Auto-poblar número de teléfono si el cliente lo tiene registrado
    LaunchedEffect(customerData, quotation) {
        if (phoneText.isEmpty()) {
            val phone = customerData?.attributes?.phone
                ?: quotation?.attributes?.customerPhone
            if (!phone.isNullOrBlank()) {
                phoneText = phone
            }
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val message = "Comprobante #${attr.referenceCode} por un total de S/ ${String.format("%.2f", attr.grandTotal)}"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            shareIntent.type = "text/plain"
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir comprobante"))
                        }) {
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

                // Sección de Teléfono/WhatsApp y botones de acción
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Número de WhatsApp / Teléfono") },
                        placeholder = { Text("Ej. 987654321") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Icono Teléfono WhatsApp",
                                tint = Color(0xFF25D366) // Color verde WhatsApp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                var cleanPhone = phoneText.trim().replace(" ", "").replace("-", "")
                                if (cleanPhone.isNotEmpty()) {
                                    // Anteponer 51 si tiene 9 dígitos (Perú)
                                    if (cleanPhone.length == 9 && !cleanPhone.startsWith("+") && !cleanPhone.startsWith("51")) {
                                        cleanPhone = "51$cleanPhone"
                                    }
                                    val refCode = attr.referenceCode
                                    val grandTotal = String.format("%.2f", attr.grandTotal)
                                    val fullNum = attr.electronicDocument?.fullNumber
                                    val docInfo = if (!fullNum.isNullOrEmpty()) " ($fullNum)" else ""
                                    val pdfUrl = attr.electronicDocument?.pdfUrl ?: ""

                                    val message = if (!pdfUrl.isNullOrEmpty()) {
                                        "Hola, le enviamos su comprobante de venta$docInfo (#$refCode) por un total de S/ $grandTotal.\n\nVer comprobante PDF: $pdfUrl"
                                    } else {
                                        "Hola, le enviamos la información de su compra$docInfo (#$refCode) por un total de S/ $grandTotal. ¡Gracias por su preferencia!"
                                    }

                                    try {
                                        val encodedMsg = URLEncoder.encode(message, "UTF-8")
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al abrir WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Por favor ingrese un número de teléfono", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // Verde WhatsApp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar", fontWeight = FontWeight.Bold, color = Color.White)
                            }
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
                            Text("Finalizar", fontWeight = FontWeight.Bold)
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
