package com.zeddihub.mobile.ui.admin

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.CredentialStore

private const val ADMIN_URL = "https://zeddihub.eu/tools/admin/"
private const val ADMIN_HOST = "zeddihub.eu"

/**
 * Returns true if [url] is on the trusted admin host. We refuse to
 * inject credentials anywhere else — a redirect off-domain would
 * otherwise leak the stored password to a third-party origin.
 */
private fun isTrustedAdminUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return runCatching {
        val parsed = Uri.parse(url)
        parsed.scheme.equals("https", ignoreCase = true) &&
            parsed.host?.equals(ADMIN_HOST, ignoreCase = true) == true
    }.getOrDefault(false)
}

@Composable
fun AdminScreen(
    padding: PaddingValues,
    credentials: CredentialStore.Credentials?
) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    // Single-shot guard: once we've successfully filled the login form,
    // never re-inject. Prevents credential leakage if the user navigates
    // to a different page within the trusted host that happens to also
    // contain a password field (e.g. user-settings).
    val credsInjected = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        // Tighten attack surface: block file://, content://
                        // and mixed (HTTP) content. WebView defaults are
                        // historically permissive — these flags lock it
                        // down to https-only content from our origin.
                        allowFileAccess = false
                        allowContentAccess = false
                        allowFileAccessFromFileURLs = false
                        allowUniversalAccessFromFileURLs = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            isLoading = true
                            hasError = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val target = request?.url?.toString().orEmpty()
                            // Keep all navigation on our origin inside
                            // the WebView; bounce everything else out to
                            // the system browser so the in-app session
                            // can never end up on third-party pages
                            // where credentials might be auto-filled.
                            return if (isTrustedAdminUrl(target)) {
                                false
                            } else if (target.isNotBlank()) {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                }
                                true
                            } else true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            // Only inject credentials when:
                            //   1) we have any to inject,
                            //   2) the page is on the trusted host (no
                            //      cross-domain redirect),
                            //   3) we haven't already injected this
                            //      session (single-shot login).
                            if (
                                credentials != null &&
                                !credsInjected.value &&
                                isTrustedAdminUrl(url)
                            ) {
                                val safeUser = credentials.username.replace("\\", "\\\\").replace("\"", "\\\"")
                                val safePass = credentials.password.replace("\\", "\\\\").replace("\"", "\\\"")
                                val js = "(function(){try{" +
                                    "var u=document.querySelector('input[type=text],input[name=username],input[name=user],input[name=login],input[id=username]');" +
                                    "var p=document.querySelector('input[type=password]');" +
                                    "if(u&&p&&!u.value){u.value=\"$safeUser\";p.value=\"$safePass\";" +
                                    "u.dispatchEvent(new Event('input',{bubbles:true}));" +
                                    "p.dispatchEvent(new Event('input',{bubbles:true}));" +
                                    "var f=p.closest('form');if(f){var s=f.querySelector('button[type=submit],input[type=submit]');" +
                                    "if(s){s.click();}else{f.submit();}}return 'filled';}return 'noform';" +
                                    "}catch(e){return 'err';}})();"
                                view?.evaluateJavascript(js) { result ->
                                    // Lock the guard once we actually
                                    // filled a form. If the page didn't
                                    // have a login form (`noform`), keep
                                    // listening — admin login is at /tools/admin/
                                    // root and we may be on a redirect.
                                    if (result?.contains("filled") == true) {
                                        credsInjected.value = true
                                    }
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                isLoading = false
                            }
                        }
                    }
                    loadUrl(ADMIN_URL)
                    webView = this
                }
            },
            update = { wv -> webView = wv }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.admin_loading), color = colors.onBackground)
                }
            }
        }

        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.admin_error),
                    color = colors.error,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    hasError = false
                    webView?.loadUrl(ADMIN_URL)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("  ${stringResource(R.string.admin_reload)}")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ADMIN_URL)))
                }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Text("  ${stringResource(R.string.admin_open_external)}")
                }
            }
        }
    }
}
