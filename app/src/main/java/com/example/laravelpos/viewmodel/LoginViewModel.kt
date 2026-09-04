package com.example.laravelpos.viewmodel

// viewmodel/LoginViewModel.kt
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.LoginRequest
import com.example.laravelpos.data.repository.LoginRepository
import com.example.laravelpos.data.repository.ProductRepository.Companion.TOKEN_KEY
import com.example.laravelpos.data.config.ServerConfig
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
    private val sharedPreferences: SharedPreferences,
    private val serverConfig: ServerConfig
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    fun getServerIp() = serverConfig.getServerIp()

    fun updateServerIp(ip: String) {
        serverConfig.setServerIp(ip)
    }

    private val _userName = MutableStateFlow<String?>(repository.getUserName())
    val userName: StateFlow<String?> = _userName

    private val _userRole = MutableStateFlow<String?>(repository.getUserRole())
    val userRole: StateFlow<String?> = _userRole

    private val _userPermissions = MutableStateFlow<List<String>>(repository.getUserPermissions())
    val userPermissions: StateFlow<List<String>> = _userPermissions.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(sharedPreferences.getString(TOKEN_KEY, null) != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        // Al iniciar, si está autenticado, refrescamos el perfil para tener el rol actualizado
        if (isLoggedIn()) {
            refreshProfile()
        }
    }

    fun refreshProfile() {
        Log.d("LoginViewModel", "refreshProfile: Starting...")
        viewModelScope.launch {
            _state.value = LoginState(isLoading = true)
            try {
                val profileSuccess = repository.fetchProfile()
                val configSuccess = repository.fetchConfig()
                Log.d("LoginViewModel", "refreshProfile: Profile: $profileSuccess, Config: $configSuccess")
                
                _userName.value = repository.getUserName()
                _userRole.value = repository.getUserRole()
                _userPermissions.value = repository.getUserPermissions()
                Log.d("LoginViewModel", "refreshProfile: User info updated. Permissions count: ${_userPermissions.value.size}")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "refreshProfile: Error: ${e.message}", e)
                _state.value = LoginState(error = e.message)
            } finally {
                _state.value = LoginState(isLoading = false)
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = LoginState(isLoading = true)
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.data != null) {
                    response.data.let {
                        _userName.value = it.user.firstName
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