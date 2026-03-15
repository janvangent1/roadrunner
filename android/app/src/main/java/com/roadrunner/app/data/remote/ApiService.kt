package com.roadrunner.app.data.remote

import com.roadrunner.app.data.remote.dto.LicenseCheckRequest
import com.roadrunner.app.data.remote.dto.LicenseCheckResponse
import com.roadrunner.app.data.remote.dto.RouteDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiService {

    @GET("api/v1/routes")
    suspend fun getRoutes(): Response<List<RouteDto>>

    @GET("api/v1/routes/{id}")
    suspend fun getRoute(@Path("id") id: String): Response<RouteDto>

    @POST("api/v1/licenses/check")
    suspend fun checkLicense(@Body body: LicenseCheckRequest): Response<LicenseCheckResponse>

    @Streaming
    @GET("api/v1/routes/{id}/gpx")
    suspend fun getRouteGpx(@Path("id") id: String): Response<ResponseBody>
}
