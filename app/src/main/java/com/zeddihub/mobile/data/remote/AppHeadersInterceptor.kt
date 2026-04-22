package com.zeddihub.mobile.data.remote

import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.data.local.CredentialStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches ZeddiHub-wide headers to every outbound request targeting the
 * ZeddiHub API host.
 *
 * - `X-App-Secret` — lets the backend skip hCaptcha for native app calls.
 *   Must match `ZH_APP_SECRET` in `website/api/_config.php`.
 * - `X-Client-Kind` — always `mobile`.
 * - `X-Client-Version` — [BuildConfig.VERSION_NAME].
 * - `Authorization: Bearer <token>` — added automatically when the user
 *   is signed in, unless the request already supplies one (e.g.
 *   `authLogout(bearer)` passes it explicitly).
 *
 * Requests to third-party hosts (e.g. `openstreetmap.org` tile fetches
 * done outside Retrofit) pass through untouched because the client uses
 * a separate OkHttp instance there. For robustness we still scope the
 * bearer header to known ZeddiHub hosts.
 */
@Singleton
class AppHeadersInterceptor @Inject constructor(
    private val credentialStore: CredentialStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host.lowercase()
        val isZeddiHost = host == "zeddihub.eu" || host.endsWith(".zeddihub.eu")

        val builder = original.newBuilder()
            .header("X-Client-Kind", BuildConfig.CLIENT_KIND)
            .header("X-Client-Version", BuildConfig.VERSION_NAME)

        if (isZeddiHost) {
            builder.header("X-App-Secret", BuildConfig.APP_SECRET)
            if (original.header("Authorization").isNullOrBlank()) {
                credentialStore.session.value?.token?.takeIf { it.isNotBlank() }?.let { token ->
                    builder.header("Authorization", "Bearer $token")
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
