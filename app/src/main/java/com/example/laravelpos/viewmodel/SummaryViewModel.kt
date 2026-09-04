package com.example.laravelpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.QuotationData
import com.example.laravelpos.data.repository.QuotationRepository
import com.example.laravelpos.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryState(
    val isLoading: Boolean = false,
    val quotation: QuotationData? = null,
    val error: String? = null
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val quotationRepository: QuotationRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SummaryState())
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    fun loadData(type: String, id: Int) {
        viewModelScope.launch {
            _state.value = SummaryState(isLoading = true)
            val result = if (type == "sale") {
                saleRepository.getSale(id)
            } else {
                quotationRepository.getQuotation(id)
            }

            if (result.success) {
                _state.value = SummaryState(quotation = result.data, isLoading = false)
            } else {
                _state.value = SummaryState(error = result.message, isLoading = false)
            }
        }
    }
}
