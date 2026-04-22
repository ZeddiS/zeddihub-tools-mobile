package com.zeddihub.mobile.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.repository.AuthError
import com.zeddihub.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginErrorKind { NONE, EMPTY, CREDENTIALS, NETWORK, DISABLED, RATE_LIMITED, GENERIC }

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val username: String = "",
        val password: String = "",
        val rememberMe: Boolean = false,
        val isLoading: Boolean = false,
        val errorKind: LoginErrorKind = LoginErrorKind.NONE,
        val loggedIn: Boolean = false,
        val biometricAvailable: Boolean = false
    )

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.session.value != null)

    val sessionFlow: StateFlow<com.zeddihub.mobile.data.local.CredentialStore.Session?> =
        authRepository.session

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun credentials(): com.zeddihub.mobile.data.local.CredentialStore.Credentials? =
        authRepository.rememberedCredentials()

    private fun initialState(): UiState {
        val remembered = authRepository.rememberedCredentials()
        return UiState(
            username = savedStateHandle.get<String>(KEY_USERNAME) ?: remembered?.username.orEmpty(),
            password = savedStateHandle.get<String>(KEY_PASSWORD).orEmpty(),
            rememberMe = savedStateHandle.get<Boolean>(KEY_REMEMBER) ?: authRepository.isRememberEnabled()
        )
    }

    fun onUsernameChange(v: String) {
        _state.value = _state.value.copy(username = v, errorKind = LoginErrorKind.NONE)
        savedStateHandle[KEY_USERNAME] = v
    }

    fun onPasswordChange(v: String) {
        _state.value = _state.value.copy(password = v, errorKind = LoginErrorKind.NONE)
        savedStateHandle[KEY_PASSWORD] = v
    }

    fun onRememberChange(v: Boolean) {
        _state.value = _state.value.copy(rememberMe = v)
        savedStateHandle[KEY_REMEMBER] = v
    }

    fun setBiometricAvailable(available: Boolean) {
        _state.value = _state.value.copy(biometricAvailable = available)
    }

    fun submit() = submitInternal(_state.value.username, _state.value.password, _state.value.rememberMe)

    fun hasRememberedCredentials(): Boolean =
        authRepository.rememberedCredentials() != null

    fun submitWithRememberedCredentials(): Boolean {
        val creds = authRepository.rememberedCredentials() ?: return false
        _state.value = _state.value.copy(
            username = creds.username,
            password = creds.password,
            rememberMe = true
        )
        submitInternal(creds.username, creds.password, true)
        return true
    }

    private fun submitInternal(username: String, password: String, rememberMe: Boolean) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(errorKind = LoginErrorKind.EMPTY)
            return
        }
        _state.value = _state.value.copy(isLoading = true, errorKind = LoginErrorKind.NONE)
        viewModelScope.launch {
            val result = authRepository.login(username.trim(), password, rememberMe)
            if (result.isSuccess) {
                appPreferences.setBiometricEnabled(rememberMe)
                _state.value = _state.value.copy(isLoading = false, loggedIn = true)
            } else {
                val kind = when (val err = result.exceptionOrNull()) {
                    is AuthError.BadCredentials,
                    is AuthError.MissingIdentifier,
                    is AuthError.MissingPassword -> LoginErrorKind.CREDENTIALS
                    is AuthError.Network -> LoginErrorKind.NETWORK
                    is AuthError.Disabled -> LoginErrorKind.DISABLED
                    is AuthError.TooFast,
                    is AuthError.TooManyFails,
                    is AuthError.DailyLimit -> LoginErrorKind.RATE_LIMITED
                    is AuthError.AuthRequired,
                    is AuthError.AuthInvalid -> LoginErrorKind.CREDENTIALS
                    else -> {
                        // Keep reference to silence unused warning while falling through
                        @Suppress("UNUSED_VARIABLE") val ignored = err
                        LoginErrorKind.GENERIC
                    }
                }
                _state.value = _state.value.copy(isLoading = false, errorKind = kind)
            }
        }
    }

    companion object {
        private const val KEY_USERNAME = "login_username"
        private const val KEY_PASSWORD = "login_password"
        private const val KEY_REMEMBER = "login_remember"
    }
}
