package com.roadrunner.app.data.remote

import com.roadrunner.app.data.remote.dto.AuthResponse
import com.roadrunner.app.data.remote.dto.GoogleSignInRequest
import com.roadrunner.app.data.remote.dto.LoginRequest
import com.roadrunner.app.data.remote.dto.LogoutRequest
import com.roadrunner.app.data.remote.dto.RefreshRequest
import com.roadrunner.app.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/google")
    suspend fun googleSignIn(@Body body: GoogleSignInRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>
}
