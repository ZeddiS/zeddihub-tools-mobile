package com.zeddihub.mobile.data.repository

import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.WifiMapEntryDto
import com.zeddihub.mobile.data.remote.dto.WifiMapSubmitRequest
import com.zeddihub.mobile.data.remote.dto.WifiMapSubmitResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiMapRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun list(
        lat: Double? = null,
        lon: Double? = null,
        radiusKm: Double? = null
    ): Result<List<WifiMapEntryDto>> = runCatching {
        api.wifiMapList(lat = lat, lon = lon, radiusKm = radiusKm).entries
    }

    suspend fun submit(
        request: WifiMapSubmitRequest
    ): Result<WifiMapSubmitResponse> = runCatching {
        api.wifiMapSubmit(request)
    }
}
