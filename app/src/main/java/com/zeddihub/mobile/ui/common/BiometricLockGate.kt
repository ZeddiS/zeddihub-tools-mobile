package com.zeddihub.mobile.ui.common

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.zeddihub.mobile.R
import java.util.concurrent.Executor

/**
 * Wraps `content` with a blocking biometric lock screen. When `locked` is
 * true, the content is hidden and the user must authenticate to continue.
 *
 * The caller owns the unlock state — this composable only requests auth and
 * reports success via [onUnlocked]. Designed to be shown on every cold start
 * of the activity (or on return from background after a timeout).
 */
@Composable
fun BiometricLockGate(
    locked: Boolean,
    onUnlocked: () -> Unit,
    content: @Composable () -> Unit
) {
    content()
    if (!locked) return

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val biometricAvailable = remember {
        val code = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        code == BiometricManager.BIOMETRIC_SUCCESS
    }

    var error by remember { mutableStateOf<String?>(null) }

    val title = stringResource(R.string.app_lock_prompt_title)
    val subtitle = stringResource(R.string.app_lock_prompt_subtitle)
    val cancel = stringResource(R.string.login_biometric_cancel)
    val errorNoBiometric = stringResource(R.string.app_lock_no_biometric)

    fun prompt() {
        val activity = context as? FragmentActivity ?: return
        if (!biometricAvailable) {
            // No biometric enrolled on the device: we don't fall back to
            // password by design (the device has its own keyguard for that).
            // Leave the user on the lock screen with a settings shortcut.
            error = errorNoBiometric
            return
        }
        val executor: Executor =
            androidx.core.content.ContextCompat.getMainExecutor(context)
        val bp = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    error = null
                    onUnlocked()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    // User cancelled or system error — keep the lock up and
                    // show the reason so they can retry.
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        error = errString.toString()
                    }
                }
            }
        )
        bp.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancel)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()
        )
    }

    // Fire the prompt once as soon as the gate appears.
    LaunchedEffect(Unit) { prompt() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.background,
                        colors.primary.copy(alpha = 0.12f),
                        colors.background
                    )
                )
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.app_lock_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.app_lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { prompt() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.app_lock_unlock))
            }
            error?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
                if (!biometricAvailable) {
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_SECURITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) {
                        Text(stringResource(R.string.app_lock_open_security))
                    }
                }
            }
        }
    }
}
