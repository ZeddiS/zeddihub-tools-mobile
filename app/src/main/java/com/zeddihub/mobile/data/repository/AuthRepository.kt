package com.zeddihub.mobile.data.repository

import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.AuthResponse
import com.zeddihub.mobile.data.remote.dto.AuthUser
import com.zeddihub.mobile.data.remote.dto.LoginRequest
import com.zeddihub.mobile.data.remote.dto.MeResponse
import com.zeddihub.mobile.data.remote.dto.RegisterRequest
import com.zeddihub.mobile.data.remote.dto.UserDto
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val credentialStore: CredentialStore
) {

    val session: StateFlow<CredentialStore.Session?> = credentialStore.session

    // -----------------------------------------------------------------
    // Login
    // -----------------------------------------------------------------
    suspend fun login(
        identifier: String,
        password: String,
        rememberMe: Boolean,
        storePassword: Boolean = rememberMe
    ): Result<AuthUser> = runAuthCall {
        api.authLogin(LoginRequest(identifier = identifier.trim(), password = password))
    }.mapCatching { response ->
        val user = persistSession(response.user!!, response.token!!, response.expiresAt ?: 0L)
        if (storePassword) {
            credentialStore.saveRememberedCredentials(identifier.trim(), password)
        } else if (!rememberMe) {
            credentialStore.clearRememberedCredentials()
        }
        user
    }

    // -----------------------------------------------------------------
    // Register
    // -----------------------------------------------------------------
    suspend fun register(
        username: String,
        email: String,
        password: String,
        rememberMe: Boolean,
        storePassword: Boolean = rememberMe
    ): Result<AuthUser> = runAuthCall {
        api.authRegister(
            RegisterRequest(
                username = username.trim(),
                email = email.trim(),
                password = password
            )
        )
    }.mapCatching { response ->
        val user = persistSession(response.user!!, response.token!!, response.expiresAt ?: 0L)
        if (storePassword) {
            credentialStore.saveRememberedCredentials(username.trim(), password)
        } else if (!rememberMe) {
            credentialStore.clearRememberedCredentials()
        }
        user
    }

    // -----------------------------------------------------------------
    // Resume session (see MOBILE_SYNC_v2.md §3.1)
    // -----------------------------------------------------------------
    /**
     * Startup routine mirroring desktop's `gui/auth.py::resume_session()`.
     *
     * Returns:
     *  - [ResumeResult.Resumed] — session verified via `/me`, expiry slid forward.
     *  - [ResumeResult.OfflineCached] — network error but cached session still
     *    valid according to local `expires_at`; caller may proceed with cached
     *    user (read-only offline mode).
     *  - [ResumeResult.ReLoggedIn] — `/me` rejected the token but we had a
     *    remembered password; we transparently called `/login` and now have
     *    a fresh session.
     *  - [ResumeResult.NeedsLogin] — no valid session, user must sign in.
     */
    suspend fun resumeSession(): ResumeResult {
        val session = credentialStore.session.value
        val token = session?.token?.takeIf { it.isNotBlank() }
            ?: return ResumeResult.NeedsLogin

        val meResult = runAuthCall { api.authMe("Bearer $token") }
        meResult.onSuccess { me ->
            me.expiresAt?.let { credentialStore.updateTokenExpiry(it) }
            return ResumeResult.Resumed
        }

        val err = meResult.exceptionOrNull()
        // Network: if the cached expiry is still in the future, allow offline use.
        if (err is AuthError.Network) {
            val now = System.currentTimeMillis() / 1000L
            return if (session.expiresAt > now) ResumeResult.OfflineCached
            else ResumeResult.NeedsLogin
        }
        // Token was explicitly rejected; try remembered password to re-login.
        if (err is AuthError.AuthRequired || err is AuthError.AuthInvalid) {
            val remembered = credentialStore.rememberedCredentials()
            if (remembered != null) {
                val loginResult = login(
                    identifier = remembered.username,
                    password = remembered.password,
                    rememberMe = true,
                    storePassword = true
                )
                if (loginResult.isSuccess) return ResumeResult.ReLoggedIn
            }
            credentialStore.clearSession()
            return ResumeResult.NeedsLogin
        }
        // Any other auth error: wipe and prompt.
        credentialStore.clearSession()
        return ResumeResult.NeedsLogin
    }

    enum class ResumeResult { Resumed, OfflineCached, ReLoggedIn, NeedsLogin }

    // -----------------------------------------------------------------
    // Manual refresh (e.g. pull-to-refresh on profile)
    // -----------------------------------------------------------------
    suspend fun refreshSession(): Boolean {
        val token = credentialStore.session.value?.token?.takeIf { it.isNotBlank() }
            ?: return false
        return runAuthCall { api.authMe("Bearer $token") }.fold(
            onSuccess = { me ->
                me.expiresAt?.let { credentialStore.updateTokenExpiry(it) }
                true
            },
            onFailure = { e ->
                if (e is AuthError.AuthRequired || e is AuthError.AuthInvalid) {
                    credentialStore.clearSession()
                }
                false
            }
        )
    }

    fun rememberedCredentials(): CredentialStore.Credentials? =
        credentialStore.rememberedCredentials()

    fun isRememberEnabled(): Boolean = credentialStore.isRememberEnabled()

    /** POST /logout (best-effort) + wipe in-memory session. Keeps remembered password. */
    suspend fun logout() {
        val token = credentialStore.session.value?.token?.takeIf { it.isNotBlank() }
        if (token != null) {
            runCatching { api.authLogout("Bearer $token") }
        }
        credentialStore.clearSession()
    }

    /** Nuke remembered credentials too (Settings → "Forget my credentials"). */
    fun forgetCredentials() {
        credentialStore.clearRememberedCredentials()
    }

    /** Fire-and-forget logout for UI layers that can't suspend. */
    fun logoutLocal() {
        credentialStore.clearSession()
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /** Single place that converts transport exceptions into [AuthError]. */
    private inline fun <T> runAuthCall(block: () -> T): Result<T> = try {
        val resp = block()
        when (resp) {
            is AuthResponse -> if (resp.ok && resp.token != null && resp.user != null) Result.success(resp)
                else Result.failure(AuthError.fromKey(resp.error, resp.message, 0))
            is MeResponse -> if (resp.ok && resp.user != null) Result.success(resp)
                else Result.failure(AuthError.fromKey(resp.error, resp.message, 0))
            else -> Result.success(resp)
        }
    } catch (e: IOException) {
        Result.failure(AuthError.Network(e.message))
    } catch (e: HttpException) {
        // Try to parse the body for a structured error key first.
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val parsed = parseErrorBody(body)
        val mapped = if (parsed != null) {
            AuthError.fromKey(parsed.first, parsed.second, e.code())
        } else {
            AuthError.fromHttpCode(e.code(), body.takeIf { it.isNotBlank() })
        }
        Result.failure(mapped)
    } catch (t: Throwable) {
        Result.failure(AuthError.Unknown(null, t.message, 0))
    }

    private fun parseErrorBody(raw: String): Pair<String?, String?>? {
        if (raw.isBlank()) return null
        val key = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
        val msg = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(raw)?.groupValues?.getOrNull(1)
        return if (key == null && msg == null) null else key to msg
    }

    private fun persistSession(dto: UserDto, token: String, expiresAt: Long): AuthUser {
        val role = dto.role ?: "user"
        val isAdmin = dto.isAdmin || role.equals("admin", ignoreCase = true)
        credentialStore.saveSession(
            userId = dto.id,
            username = dto.username,
            email = dto.email,
            role = role,
            isAdmin = isAdmin,
            token = token,
            expiresAt = expiresAt,
            displayName = dto.displayName,
            avatarUrl = dto.avatarUrl
        )
        return AuthUser(
            id = dto.id,
            username = dto.username,
            email = dto.email,
            role = role,
            isAdmin = isAdmin,
            displayName = dto.displayName,
            avatarUrl = dto.avatarUrl
        )
    }
}
