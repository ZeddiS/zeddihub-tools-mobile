package com.zeddihub.mobile.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Retries idempotent (and explicitly retry-marked) requests on transient
 * failures using a bounded exponential backoff.
 *
 * What we retry:
 *   - IOExceptions thrown by the OkHttp call (DNS hiccups, conn reset,
 *     read timeout on a fresh connection)
 *   - HTTP 408 (request timeout)
 *   - HTTP 425 (too early)
 *   - HTTP 429 (rate-limit) — honours the `Retry-After` header if present
 *   - HTTP 502 / 503 / 504 (gateway / upstream blips)
 *
 * What we DO NOT retry:
 *   - 4xx other than 408/425/429 — those are caller errors
 *   - 5xx other than the gateway family — likely real bugs, retrying
 *     just hammers the server
 *   - Non-idempotent verbs (POST/PATCH) **unless** the caller opts in
 *     with the `X-Retry-Safe: 1` header, which we strip before sending.
 *     This makes the policy explicit at the call site instead of
 *     guessing.
 *
 * Backoff: 250 ms → 500 ms → 1 s, max 3 attempts. Capped at the
 * server's `Retry-After` value when it asks for longer (clamped to
 * 10 s so a misconfigured server can't hang a screen).
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val optInHeader = original.header(HEADER_RETRY_SAFE)
        val request = if (optInHeader != null) {
            original.newBuilder().removeHeader(HEADER_RETRY_SAFE).build()
        } else {
            original
        }

        val method = request.method.uppercase()
        val isIdempotent = method == "GET" || method == "HEAD" || method == "OPTIONS"
        val canRetry = isIdempotent || optInHeader == "1"

        var attempt = 0
        var lastFailure: IOException? = null
        var lastResponse: Response? = null

        while (attempt < MAX_ATTEMPTS) {
            // Close the previous response (if any) before issuing the
            // next attempt — leaking response bodies blocks the connection
            // pool and we'd run out of sockets fast.
            lastResponse?.close()
            lastResponse = null
            lastFailure = null

            try {
                val response = chain.proceed(request)
                if (!canRetry || !isRetriableStatus(response.code) || attempt == MAX_ATTEMPTS - 1) {
                    return response
                }
                val sleepMs = retryAfterMs(response) ?: backoffMs(attempt)
                lastResponse = response
                sleep(sleepMs)
            } catch (e: IOException) {
                if (!canRetry || attempt == MAX_ATTEMPTS - 1) throw e
                lastFailure = e
                sleep(backoffMs(attempt))
            }
            attempt++
        }

        // Unreachable in practice — the loop above always returns or
        // throws on the last attempt. Defensive fallback.
        lastResponse?.let { return it }
        throw lastFailure ?: IOException("Retry budget exhausted")
    }

    private fun isRetriableStatus(code: Int): Boolean =
        code == 408 || code == 425 || code == 429 ||
            code == 502 || code == 503 || code == 504

    private fun backoffMs(attempt: Int): Long = when (attempt) {
        0 -> 250L
        1 -> 500L
        else -> 1000L
    }

    private fun retryAfterMs(response: Response): Long? {
        val raw = response.header("Retry-After") ?: return null
        // Retry-After can be either delta-seconds or HTTP-date. We only
        // bother with the seconds form — the date form is rare and the
        // backoff fallback is fine.
        val seconds = raw.trim().toLongOrNull() ?: return null
        if (seconds <= 0L) return null
        return min(seconds * 1000L, MAX_RETRY_AFTER_MS)
    }

    private fun sleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val MAX_RETRY_AFTER_MS = 10_000L

        /**
         * Opt-in header callers can set to mark a non-idempotent request
         * as safe to retry (e.g. POSTs with a server-side idempotency
         * key). Stripped from the actual outbound request.
         */
        const val HEADER_RETRY_SAFE = "X-Retry-Safe"
    }
}
