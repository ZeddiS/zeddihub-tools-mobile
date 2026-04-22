package com.zeddihub.mobile.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.repository.AuthError
import com.zeddihub.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RegisterErrorKind {
    NONE, EMPTY, PASSWORD_MISMATCH, INVALID_USERNAME, INVALID_EMAIL,
    INVALID_PASSWORD, TAKEN, RATE_LIMITED, NETWORK, GENERIC
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val username: String = "",
        val email: String = "",
        val password: String = "",
        val passwordConfirm: String = "",
        val rememberMe: Boolean = false,
        val isLoading: Boolean = false,
        val errorKind: RegisterErrorKind = RegisterErrorKind.NONE,
        val registered: Boolean = false
    )

    private val _state = MutableStateFlow(
        UiState(
            username = savedStateHandle.get<String>(KEY_USERNAME).orEmpty(),
            email = savedStateHandle.get<String>(KEY_EMAIL).orEmpty()
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) {
        _state.value = _state.value.copy(username = v, errorKind = RegisterErrorKind.NONE)
        savedStateHandle[KEY_USERNAME] = v
    }

    fun onEmailChange(v: String) {
        _state.value = _state.value.copy(email = v, errorKind = RegisterErrorKind.NONE)
        savedStateHandle[KEY_EMAIL] = v
    }

    fun onPasswordChange(v: String) {
        _state.value = _state.value.copy(password = v, errorKind = RegisterErrorKind.NONE)
    }

    fun onPasswordConfirmChange(v: String) {
        _state.value = _state.value.copy(passwordConfirm = v, errorKind = RegisterErrorKind.NONE)
    }

    fun onRememberChange(v: Boolean) {
        _state.value = _state.value.copy(rememberMe = v)
    }

    fun submit() {
        val s = _state.value
        if (s.username.isBlank() || s.email.isBlank() || s.password.isBlank() || s.passwordConfirm.isBlank()) {
            _state.value = s.copy(errorKind = RegisterErrorKind.EMPTY)
            return
        }
        // Client-side validation mirroring server rules (MOBILE_SYNC_v2 §8 +
        // _config.php ZH_USERNAME_*/ZH_PASSWORD_* policy constants).
        val trimmedUser = s.username.trim()
        if (!USERNAME_REGEX.matches(trimmedUser)) {
            _state.value = s.copy(errorKind = RegisterErrorKind.INVALID_USERNAME)
            return
        }
        if (!EMAIL_REGEX.matches(s.email.trim())) {
            _state.value = s.copy(errorKind = RegisterErrorKind.INVALID_EMAIL)
            return
        }
        if (s.password.length !in PASSWORD_MIN..PASSWORD_MAX) {
            _state.value = s.copy(errorKind = RegisterErrorKind.INVALID_PASSWORD)
            return
        }
        if (s.password != s.passwordConfirm) {
            _state.value = s.copy(errorKind = RegisterErrorKind.PASSWORD_MISMATCH)
            return
        }
        _state.value = s.copy(isLoading = true, errorKind = RegisterErrorKind.NONE)
        viewModelScope.launch {
            val result = authRepository.register(
                username = s.username.trim(),
                email = s.email.trim(),
                password = s.password,
                rememberMe = s.rememberMe
            )
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, registered = true)
            } else {
                val kind = when (result.exceptionOrNull()) {
                    is AuthError.InvalidUsername -> RegisterErrorKind.INVALID_USERNAME
                    is AuthError.InvalidEmail -> RegisterErrorKind.INVALID_EMAIL
                    is AuthError.InvalidPassword -> RegisterErrorKind.INVALID_PASSWORD
                    is AuthError.Taken -> RegisterErrorKind.TAKEN
                    is AuthError.TooFast,
                    is AuthError.TooManyFails,
                    is AuthError.DailyLimit -> RegisterErrorKind.RATE_LIMITED
                    is AuthError.Network -> RegisterErrorKind.NETWORK
                    else -> RegisterErrorKind.GENERIC
                }
                _state.value = _state.value.copy(isLoading = false, errorKind = kind)
            }
        }
    }

    companion object {
        private const val KEY_USERNAME = "register_username"
        private const val KEY_EMAIL = "register_email"
        // Mirrors server `^[A-Za-z0-9_.\-]{3,24}$` in api/auth/_bootstrap.php.
        private val USERNAME_REGEX = Regex("^[A-Za-z0-9_.\\-]{3,24}$")
        // Lightweight email check — server does the authoritative validation.
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
        private const val PASSWORD_MIN = 8
        private const val PASSWORD_MAX = 128
    }
}
