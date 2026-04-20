package com.zeddihub.mobile.data.remote

import com.zeddihub.mobile.data.remote.dto.AuthJsonDto
import retrofit2.http.GET

interface ApiService {

    @GET("tools/data/auth.json")
    suspend fun fetchAuth(): AuthJsonDto
}
