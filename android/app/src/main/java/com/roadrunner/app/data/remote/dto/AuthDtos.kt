package com.roadrunner.app.data.remote.dto

data class RegisterRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleSignInRequest(val idToken: String)
data class RefreshRequest(val refreshToken: String)
data class LogoutRequest(val refreshToken: String)
data class AuthResponse(val accessToken: String, val refreshToken: String)
