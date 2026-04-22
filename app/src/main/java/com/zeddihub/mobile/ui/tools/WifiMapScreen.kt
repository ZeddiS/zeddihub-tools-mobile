package com.zeddihub.mobile.ui.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.remote.dto.WifiMapEntryDto
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun WifiMapScreen(
    padding: PaddingValues,
    vm: WifiMapViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val state by vm.state.collectAsState()
    var showSubmit by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) requestLocation(ctx) { lat, lon -> vm.setLocation(lat, lon) }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && state.userLat == null) {
            requestLocation(ctx) { lat, lon -> vm.setLocation(lat, lon) }
        } else if (!hasLocationPermission) {
            vm.refresh() // still load entries, just without user-location filter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.size(10.dp))
                        Text(
                            stringResource(R.string.wifi_map_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.primary)
                        }
                    }
                    Text(
                        stringResource(R.string.wifi_map_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (!hasLocationPermission) {
                PermissionPrompt(
                    onGrant = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                OsmMap(
                    entries = state.entries,
                    userLat = state.userLat,
                    userLon = state.userLon,
                    modifier = Modifier.fillMaxSize()
                )

                if (state.isLoading) {
                    Text(
                        stringResource(R.string.wifi_map_loading),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(colors.surface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = colors.onSurface,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (state.userLat == null) {
                            Toast.makeText(
                                ctx,
                                ctx.getString(R.string.wifi_map_submit_gps_missing),
                                Toast.LENGTH_SHORT
                            ).show()
                            if (hasLocationPermission) requestLocation(ctx) { lat, lon -> vm.setLocation(lat, lon) }
                        } else {
                            showSubmit = true
                        }
                    },
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.wifi_map_add))
                }

                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = { requestLocation(ctx) { lat, lon -> vm.setLocation(lat, lon) } },
                        containerColor = colors.surface,
                        contentColor = colors.primary,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, null)
                    }
                }
            }
        }
    }

    if (showSubmit) {
        SubmitDialog(
            submitting = state.submitting,
            onDismiss = { showSubmit = false; vm.clearSubmitFlags() },
            onSubmit = { ssid, pwd, isOpen, venue, note ->
                vm.submit(ssid, pwd, isOpen, venue, note)
            }
        )
    }

    if (state.submitSuccess) {
        LaunchedEffect(Unit) {
            Toast.makeText(ctx, ctx.getString(R.string.wifi_map_submit_ok), Toast.LENGTH_SHORT).show()
            showSubmit = false
            vm.clearSubmitFlags()
        }
    }
    state.submitError?.let { err ->
        LaunchedEffect(err) {
            val msg = if (err == "gps") R.string.wifi_map_submit_gps_missing else R.string.wifi_map_submit_error
            Toast.makeText(ctx, ctx.getString(msg), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = colors.primary)
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(R.string.wifi_map_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.wifi_map_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onGrant, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.wifi_map_permission_grant))
            }
        }
    }
}

@Composable
private fun OsmMap(
    entries: List<WifiMapEntryDto>,
    userLat: Double?,
    userLon: Double?,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = BuildConfig.APPLICATION_ID
            osmdroidBasePath = ctx.cacheDir
            osmdroidTileCache = ctx.cacheDir
        }
    }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(50.0755, 14.4378)) // Prague default
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    LaunchedEffect(userLat, userLon) {
        if (userLat != null && userLon != null) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
            mapView.controller.setZoom(15.5)
        }
    }

    LaunchedEffect(entries, userLat, userLon) {
        mapView.overlays.clear()
        entries.forEach { e ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(e.lat, e.lon)
                title = e.ssid
                snippet = buildString {
                    if (e.isOpen) append(ctx.getString(R.string.wifi_map_entry_open))
                    else append(ctx.getString(R.string.wifi_map_entry_password_hidden))
                    if (!e.venue.isNullOrBlank()) append(" · ").append(e.venue)
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }
        if (userLat != null && userLon != null) {
            val here = Marker(mapView).apply {
                position = GeoPoint(userLat, userLon)
                title = "You"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(here)
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { /* state handled via LaunchedEffect */ }
    )
}

@Composable
private fun SubmitDialog(
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ssid: String, password: String?, isOpen: Boolean, venue: String?, note: String?) -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isOpen by remember { mutableStateOf(false) }
    var venue by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WifiPassword, null) },
        title = { Text(stringResource(R.string.wifi_map_submit_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    stringResource(R.string.wifi_map_submit_subtitle),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text(stringResource(R.string.wifi_map_submit_ssid)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isOpen, onCheckedChange = { isOpen = it })
                    Text(stringResource(R.string.wifi_map_submit_open))
                }
                if (!isOpen) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.wifi_map_submit_password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text(stringResource(R.string.wifi_map_submit_venue)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.wifi_map_submit_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !submitting && ssid.isNotBlank(),
                onClick = { onSubmit(ssid, password, isOpen, venue, note) }
            ) {
                Text(stringResource(R.string.wifi_map_submit_send))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.wifi_map_submit_cancel))
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun requestLocation(ctx: Context, onResult: (Double, Double) -> Unit) {
    val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return
    val client = LocationServices.getFusedLocationProviderClient(ctx)
    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { loc ->
            if (loc != null) onResult(loc.latitude, loc.longitude)
        }
}
