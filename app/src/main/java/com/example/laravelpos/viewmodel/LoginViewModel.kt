package com.example.laravelpos.viewmodel

// viewmodel/LoginViewModel.kt
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.LoginRequest
import com.example.laravelpos.data.repository.LoginRepository
import com.example.laravelpos.data.repository.ProductRepository.Companion.TOKEN_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: LoginRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    private val _userName = MutableStateFlow<String?>(repository.getUserName())
    val userName: StateFlow<String?> = _userName

    private val _isAuthenticated = MutableStateFlow(sharedPreferences.getString(TOKEN_KEY, null) != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = LoginState(isLoading = true)
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.data != null) {
                    response.data.let {
                        _userName.value = it.user.first_name
                        onSuccess()
                    } ?: run {
                        _state.value = LoginState(error = "No data in response")
                    }
                } else {
                    _state.value = LoginState(error = response.message ?: "Login failed")
                }
            } catch (e: Exception) {
                _state.value = LoginState(error = e.message)
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }

    fun logout() {
        repository.logout()
        _userName.value = null
    }
}