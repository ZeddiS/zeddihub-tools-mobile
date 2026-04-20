package com.zeddihub.mobile.ui.common

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeddihub.mobile.R

@Composable
fun PermissionGate(
    permissions: List<String>,
    rationale: String,
    autoRequest: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val allGranted = remember(context, permissions) {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    var denied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = permissions.all { result[it] == true || result[it] == null &&
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        allGranted.value = granted
        denied = !granted
    }

    LaunchedEffect(Unit) {
        if (!allGranted.value && autoRequest) {
            launcher.launch(permissions.toTypedArray())
        }
    }

    if (allGranted.value) {
        content()
    } else {
        val colors = MaterialTheme.colorScheme
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock, null,
                    tint = colors.primary, modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResourceSafe(R.string.permission_required_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    rationale,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { launcher.launch(permissions.toTypedArray()) },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResourceSafe(R.string.permission_grant)) }
                if (denied) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${context.packageName}"))
                        (context as? Activity)?.startActivity(intent) ?: run {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }) {
                        Text(stringResourceSafe(R.string.permission_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun stringResourceSafe(id: Int): String =
    androidx.compose.ui.res.stringResource(id)

object Permissions {
    val WIFI_SCAN = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val CAMERA = listOf(Manifest.permission.CAMERA)
    val MICROPHONE = listOf(Manifest.permission.RECORD_AUDIO)
}
