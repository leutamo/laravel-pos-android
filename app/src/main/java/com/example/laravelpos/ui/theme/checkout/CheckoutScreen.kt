package com.example.laravelpos.ui.theme.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.laravelpos.viewmodel.HomeViewModel

@Composable
fun CheckoutScreen(navController: NavController, homeViewModel: HomeViewModel) {
    val selectedReceiptType by homeViewModel.selectedReceiptType.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Checkout - Tipo de Comprobante: ${selectedReceiptType ?: "No seleccionado"}")
        Button(
            onClick = {
                navController.popBackStack() // Retrocede a "cart"
                homeViewModel.selectReceiptType(null)
            }
        ) {
            Text("Volver")
        }
    }
}