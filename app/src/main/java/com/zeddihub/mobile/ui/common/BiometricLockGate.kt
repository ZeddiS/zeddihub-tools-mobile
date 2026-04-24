package com.zeddihub.mobile.ui.common

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.R
import java.util.concurrent.Executor

/**
 * Wraps [content] with a blocking lock screen. When [locked] is true the
 * real app content is **not** composed — only a login-style lock page is
 * shown so nothing from the app leaks behind the system biometric dialog
 * (or into the recent-apps preview).
 *
 * Unlock priority:
 *  1. Biometrics (if enrolled).
 *  2. PIN fallback — if a PIN has been configured OR the device has no
 *     biometrics enrolled. The user can also tap "Use PIN" to switch from
 *     the biometric flow to the keypad.
 *  3. Link to system Security settings when nothing is configured.
 */
@Composable
fun BiometricLockGate(
    locked: Boolean,
    pinConfigured: Boolean,
    onUnlocked: () -> Unit,
    verifyPin: (String) -> Boolean,
    content: @Composable () -> Unit
) {
    if (!locked) {
        content()
        return
    }

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val biometricAvailable = remember {
        val code = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        code == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Start on the PIN keypad when there's no biometric option on the
    // device but the user has configured a PIN. Otherwise start on the
    // biometric flow.
    var showPin by remember { mutableStateOf(!biometricAvailable && pinConfigured) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = stringResource(R.string.app_lock_prompt_title)
    val subtitle = stringResource(R.string.app_lock_prompt_subtitle)
    val cancel = stringResource(R.string.login_biometric_cancel)
    val errorNoBiometric = stringResource(R.string.app_lock_no_biometric)
    val wrongPinMessage = stringResource(R.string.app_lock_pin_wrong)

    fun prompt() {
        val activity = context as? FragmentActivity ?: return
        if (!biometricAvailable) {
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

    // Fire the biometric prompt automatically when we're on the biometric
    // screen AND a credential is enrolled. Skipping this when showPin is
    // true keeps the system dialog from covering the keypad.
    LaunchedEffect(showPin, biometricAvailable) {
        if (!showPin && biometricAvailable) prompt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.background,
                        colors.primary.copy(alpha = 0.10f),
                        colors.background
                    )
                )
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.logo_banner),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(140.dp)
            )

            Spacer(Modifier.height(20.dp))

            if (showPin) {
                PinUnlockPane(
                    onSubmit = { pin ->
                        if (verifyPin(pin)) {
                            pinError = null
                            onUnlocked()
                            true
                        } else {
                            pinError = wrongPinMessage
                            false
                        }
                    },
                    error = pinError,
                    onSwitchToBiometric = if (biometricAvailable) {
                        { showPin = false }
                    } else null
                )
            } else {
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

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { prompt() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.app_lock_unlock),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (pinConfigured) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { showPin = true }) {
                        Icon(
                            Icons.Default.Dialpad,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.app_lock_use_pin))
                    }
                }

                error?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                    if (!biometricAvailable && !pinConfigured) {
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

        Text(
            text = stringResource(R.string.app_version_label, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * The PIN-only half of the lock gate. Buffers up to 12 digits. Submits
 * automatically at 12 digits (the maximum); shorter PINs are submitted by
 * the explicit "Submit" button — we never know the exact length because
 * users may choose anywhere from 4 to 12.
 */
@Composable
private fun PinUnlockPane(
    onSubmit: (String) -> Boolean,
    error: String?,
    onSwitchToBiometric: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    var buffer by remember { mutableStateOf("") }

    Text(
        stringResource(R.string.app_lock_pin_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = colors.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        stringResource(R.string.app_lock_pin_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant
    )

    Spacer(Modifier.height(18.dp))
    // Min 4 placeholder dots (matches minimum PIN length); grows as the
    // user types longer PINs, up to 12.
    val dotsTotal = buffer.length.coerceAtLeast(4).coerceAtMost(12)
    PinDots(filled = buffer.length.coerceAtMost(12), total = dotsTotal)

    Spacer(Modifier.height(20.dp))

    PinKeypad(
        onDigit = { d ->
            if (buffer.length < 12) buffer += d.toString()
            if (buffer.length == 12) {
                if (!onSubmit(buffer)) buffer = ""
            }
        },
        onBackspace = {
            if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
        }
    )

    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                if (buffer.length in 4..12) {
                    if (!onSubmit(buffer)) buffer = ""
                }
            },
            enabled = buffer.length in 4..12,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                stringResource(R.string.pin_submit),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    error?.let {
        Spacer(Modifier.height(10.dp))
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = colors.error
        )
    }

    if (onSwitchToBiometric != null) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSwitchToBiometric) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.app_lock_use_biometric))
        }
    }
}
