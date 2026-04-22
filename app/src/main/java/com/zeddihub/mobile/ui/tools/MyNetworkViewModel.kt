package com.zeddihub.mobile.ui.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import javax.inject.Inject

/**
 * "Moje Síť" tool — shows the connection the device is currently using (Wi-Fi
 * or cellular) with as much detail as Android exposes without elevated
 * permissions. Refresh is manual (pull-to-refresh or button).
 */
@HiltViewModel
class MyNetworkViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val telemetry: TelemetryRecorder
) : ViewModel() {

    enum class Transport { WIFI, CELLULAR, ETHERNET, VPN, NONE }

    data class WifiInfo(
        val ssid: String?,
        val bssid: String?,
        val rssi: Int?,            // dBm
        val signalBars: Int?,      // 0..4
        val linkSpeedMbps: Int?,
        val frequencyMhz: Int?,
        val channel: Int?,
        val band: String?,         // "2.4 GHz" / "5 GHz" / "6 GHz"
        val security: String?,     // best-effort guess
        val hiddenSsid: Boolean
    )

    data class IpInfo(
        val ipv4: String?,
        val ipv6: String?,
        val gateway: String?,
        val dns: List<String>,
        val domains: String?,
        val isMetered: Boolean
    )

    data class CellularInfo(
        val carrier: String?,
        val simOperator: String?,
        val networkType: String?, // "4G", "5G NSA", "5G SA", "3G", ...
        val mcc: String?,
        val mnc: String?,
        val roaming: Boolean
    )

    data class UiState(
        val loading: Boolean = true,
        val transport: Transport = Transport.NONE,
        val isConnected: Boolean = false,
        val downstreamKbps: Int? = null,
        val upstreamKbps: Int? = null,
        val wifi: WifiInfo? = null,
        val ip: IpInfo? = null,
        val cellular: CellularInfo? = null,
        val locationPermissionMissing: Boolean = false,
        val refreshedAt: Long = 0L
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val snapshot = withContext(Dispatchers.IO) { capture() }
            _state.value = snapshot.copy(
                loading = false,
                refreshedAt = System.currentTimeMillis()
            )
            telemetry.toolRun("my_network_refresh", true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun capture(): UiState {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return UiState(loading = false)

        val active: Network? = cm.activeNetwork
        val caps: NetworkCapabilities? = cm.getNetworkCapabilities(active)
        val link: LinkProperties? = cm.getLinkProperties(active)

        if (active == null || caps == null) {
            return UiState(loading = false, isConnected = false)
        }

        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Transport.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> Transport.VPN
            else -> Transport.NONE
        }

        val wifi = if (transport == Transport.WIFI) readWifi() else null
        val cellular = if (transport == Transport.CELLULAR) readCellular() else null
        val ip = link?.let { readIp(it, caps) }

        return UiState(
            loading = false,
            transport = transport,
            isConnected = true,
            downstreamKbps = caps.linkDownstreamBandwidthKbps.takeIf { it > 0 },
            upstreamKbps = caps.linkUpstreamBandwidthKbps.takeIf { it > 0 },
            wifi = wifi,
            ip = ip,
            cellular = cellular,
            locationPermissionMissing = transport == Transport.WIFI && !hasLocationPermission()
        )
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun readWifi(): WifiInfo {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = wifi?.connectionInfo
        val ssidRaw = info?.ssid?.trim('"')
        val ssid = if (!ssidRaw.isNullOrBlank() && ssidRaw != "<unknown ssid>") ssidRaw else null
        val bssid = info?.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
        val rssi = info?.rssi
        val bars = rssi?.let { WifiManager.calculateSignalLevel(it, 5) }
        val freq = info?.frequency
        val channel = freq?.let { frequencyToChannel(it) }
        val band = freq?.let { freqToBand(it) }
        return WifiInfo(
            ssid = ssid,
            bssid = bssid,
            rssi = rssi,
            signalBars = bars,
            linkSpeedMbps = info?.linkSpeed?.takeIf { it > 0 },
            frequencyMhz = freq?.takeIf { it > 0 },
            channel = channel,
            band = band,
            security = null, // WifiManager doesn't expose capabilities of active connection
            hiddenSsid = info?.hiddenSSID == true
        )
    }

    private fun readIp(link: LinkProperties, caps: NetworkCapabilities): IpInfo {
        val v4 = link.linkAddresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress
        val v6 = link.linkAddresses.firstOrNull { it.address is Inet6Address && !it.address.isLinkLocalAddress }
            ?.address?.hostAddress?.substringBefore('%')
        val gw = link.routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
        val dns = link.dnsServers.map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
        return IpInfo(
            ipv4 = v4,
            ipv6 = v6,
            gateway = gw,
            dns = dns,
            domains = link.domains,
            isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        )
    }

    @SuppressLint("MissingPermission")
    private fun readCellular(): CellularInfo {
        val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return CellularInfo(null, null, null, null, null, false)
        val simOp = tm.simOperator
        val carrier = tm.networkOperatorName?.takeIf { it.isNotBlank() }
        val mcc = simOp?.take(3)?.takeIf { it.length == 3 }
        val mnc = simOp?.drop(3)?.takeIf { it.isNotBlank() }
        val networkType = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dataType = tm.dataNetworkType
                networkTypeName(dataType)
            } else {
                networkTypeName(tm.networkType)
            }
        }.getOrNull()
        return CellularInfo(
            carrier = carrier,
            simOperator = tm.simOperatorName?.takeIf { it.isNotBlank() },
            networkType = networkType,
            mcc = mcc,
            mnc = mnc,
            roaming = tm.isNetworkRoaming
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun frequencyToChannel(freq: Int): Int? = when {
        freq in 2412..2472 -> (freq - 2412) / 5 + 1
        freq == 2484 -> 14
        freq in 5170..5825 -> (freq - 5000) / 5
        freq in 5955..7115 -> (freq - 5950) / 5           // 6 GHz band
        else -> null
    }

    private fun freqToBand(freq: Int): String = when {
        freq < 3000 -> "2.4 GHz"
        freq in 5000..5999 -> "5 GHz"
        freq >= 6000 -> "6 GHz"
        else -> "${freq} MHz"
    }

    private fun networkTypeName(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
        TelephonyManager.NETWORK_TYPE_NR -> "5G SA"
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G HSPA+"
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
        TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
        TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi Calling"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Neznámý"
        else -> "Type $type"
    }
}
