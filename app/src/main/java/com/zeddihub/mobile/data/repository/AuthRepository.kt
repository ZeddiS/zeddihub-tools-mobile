package com.zeddihub.mobile.data.repository

import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.AuthUser
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoginError : Exception() {
    data object InvalidCredentials : LoginError()
    data object Network : LoginError()
    data class Unknown(override val message: String?) : LoginError()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val credentialStore: CredentialStore
) {

    val session: StateFlow<CredentialStore.Session?> = credentialStore.session

    suspend fun login(
        username: String,
        password: String,
        rememberMe: Boolean
    ): Result<AuthUser> {
        val payload = try {
            api.fetchAuth()
        } catch (e: IOException) {
            return Result.failure(LoginError.Network)
        } catch (t: Throwable) {
            return Result.failure(LoginError.Unknown(t.message))
        }

        val match = payload.users.firstOrNull {
            it.username.equals(username, ignoreCase = true) && it.password == password
        } ?: return Result.failure(LoginError.InvalidCredentials)

        val user = AuthUser(
            username = match.username,
            role = match.role ?: "user",
            displayName = match.displayName,
            avatarUrl = match.avatarUrl
        )

        credentialStore.saveSession(
            username = user.username,
            role = user.role,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl
        )

        if (rememberMe) {
            credentialStore.saveRememberedCredentials(username, password)
        } else {
            credentialStore.clearRememberedCredentials()
        }

        return Result.success(user)
    }

    fun rememberedCredentials(): CredentialStore.Credentials? =
        credentialStore.rememberedCredentials()

    fun isRememberEnabled(): Boolean = credentialStore.isRememberEnabled()

    fun logout() {
        credentialStore.clearSession()
        credentialStore.clearRememberedCredentials()
    }
}
