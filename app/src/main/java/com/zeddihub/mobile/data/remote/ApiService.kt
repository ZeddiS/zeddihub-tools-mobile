package com.zeddihub.mobile.data.remote

import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.data.remote.dto.AuthJsonDto
import com.zeddihub.mobile.data.remote.dto.AuthResponse
import com.zeddihub.mobile.data.remote.dto.HomeConfigDto
import com.zeddihub.mobile.data.remote.dto.LoginRequest
import com.zeddihub.mobile.data.remote.dto.LogoutResponse
import com.zeddihub.mobile.data.remote.dto.MeResponse
import com.zeddihub.mobile.data.remote.dto.RegisterRequest
import com.zeddihub.mobile.data.remote.dto.WifiMapListDto
import com.zeddihub.mobile.data.remote.dto.WifiMapSubmitRequest
import com.zeddihub.mobile.data.remote.dto.WifiMapSubmitResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * All relative paths are resolved against [BuildConfig.API_BASE_URL] which
 * points to `https://zeddihub.eu/api/`.
 *
 * Auth endpoints live under `auth/` and return [AuthResponse] /
 * [MeResponse] with `{ ok: true, ... }` on success or `{ ok: false,
 * error: "...", message: "..." }` on failure. See
 * `website/api/auth/CONTRACT.md` for the full contract.
 */
interface ApiService {

    // -------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------

    @POST("auth/register")
    suspend fun authRegister(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun authLogin(@Body request: LoginRequest): AuthResponse

    @POST("auth/logout")
    suspend fun authLogout(@Header("Authorization") bearer: String): LogoutResponse

    @GET("auth/me")
    suspend fun authMe(@Header("Authorization") bearer: String): MeResponse

    // -------------------------------------------------------------------
    // Legacy auth.json fallback (kept for data migration / emergency).
    // -------------------------------------------------------------------

    /** Legacy static file, outside `/api/`. Caller passes [LEGACY_AUTH_URL]. */
    @GET
    suspend fun fetchAuthAt(@Url url: String): AuthJsonDto

    // -------------------------------------------------------------------
    // WiFi map
    // -------------------------------------------------------------------

    @GET("wifi-map/list")
    suspend fun wifiMapList(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radius_km") radiusKm: Double? = null
    ): WifiMapListDto

    @POST("wifi-map/submit")
    suspend fun wifiMapSubmit(@Body request: WifiMapSubmitRequest): WifiMapSubmitResponse

    // -------------------------------------------------------------------
    // Admin-managed Home config (shortcuts + news). Served as a static
    // JSON file from the website; editable via the admin panel.
    // -------------------------------------------------------------------

    @GET
    suspend fun fetchHomeConfigAt(@Url url: String): HomeConfigDto

    companion object {
        val LEGACY_AUTH_URL: String = BuildConfig.SITE_BASE_URL + "tools/data/auth.json"
        val HOME_CONFIG_URL: String = BuildConfig.SITE_BASE_URL + "tools/data/home_android.json"
    }
}

/** Convenience wrapper around [ApiService.fetchAuthAt]. */
suspend fun ApiService.fetchAuth(): AuthJsonDto =
    fetchAuthAt(ApiService.LEGACY_AUTH_URL)

/** Convenience wrapper that targets the admin-managed Home config file. */
suspend fun ApiService.fetchHomeConfig(): HomeConfigDto =
    fetchHomeConfigAt(ApiService.HOME_CONFIG_URL)
