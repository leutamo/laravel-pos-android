package com.example.laravelpos.viewmodel

// viewmodel/LoginViewModel.kt
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laravelpos.data.model.BillingCompany
import com.example.laravelpos.data.model.LoginRequest
import com.example.laravelpos.data.repository.BillingCompanyRepository
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
    private val billingCompanyRepository: BillingCompanyRepository,
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

    // Multi-empresa states
    private val _billingCompanies = MutableStateFlow<List<BillingCompany>>(emptyList())
    val billingCompanies: StateFlow<List<BillingCompany>> = _billingCompanies.asStateFlow()

    private val _activeCompany = MutableStateFlow<BillingCompany?>(null)
    val activeCompany: StateFlow<BillingCompany?> = _activeCompany.asStateFlow()

    private val _isLoadingCompanies = MutableStateFlow(false)
    val isLoadingCompanies: StateFlow<Boolean> = _isLoadingCompanies.asStateFlow()

    private val _companyError = MutableStateFlow<String?>(null)
    val companyError: StateFlow<String?> = _companyError.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        if (key == TOKEN_KEY) {
            val hasToken = sp.getString(TOKEN_KEY, null) != null
            _isAuthenticated.value = hasToken
            if (!hasToken) {
                _userName.value = null
                _userRole.value = null
                _userPermissions.value = emptyList()
                _activeCompany.value = null
                _billingCompanies.value = emptyList()
            }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
        // Al iniciar, si está autenticado, refrescamos el perfil para tener el rol actualizado
        if (isLoggedIn()) {
            refreshProfile()
            loadBillingCompanies()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    fun loadBillingCompanies() {
        viewModelScope.launch {
            _isLoadingCompanies.value = true
            _companyError.value = null
            try {
                val companies = billingCompanyRepository.getBillingCompanies()
                _billingCompanies.value = companies

                val savedId = billingCompanyRepository.getSavedActiveCompanyId()
                var current = companies.find { it.id == savedId }
                if (current == null) {
                    current = companies.find { it.isDefault == true } ?: companies.firstOrNull()
                }
                if (current != null) {
                    _activeCompany.value = current
                    billingCompanyRepository.saveSelectedCompany(current)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error loading companies: ${e.message}")
                _companyError.value = e.message
            } finally {
                _isLoadingCompanies.value = false
            }
        }
    }

    fun selectActiveCompany(company: BillingCompany) {
        viewModelScope.launch {
            _isLoadingCompanies.value = true
            _companyError.value = null
            val result = billingCompanyRepository.setActiveCompany(company.id)
            result.onSuccess { updatedCompany ->
                _activeCompany.value = updatedCompany
                Log.d("LoginViewModel", "Empresa activa cambiada exitosamente a ${updatedCompany.name}")
            }.onFailure { err ->
                Log.e("LoginViewModel", "Error cambiando empresa activa: ${err.message}")
                _companyError.value = err.message ?: "Error al cambiar empresa activa"
            }
            _isLoadingCompanies.value = false
        }
    }

    fun clearCompanyError() {
        _companyError.value = null
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
                loadBillingCompanies()
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
                        loadBillingCompanies()
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
        _activeCompany.value = null
        _billingCompanies.value = emptyList()
    }
}
