package com.roadrunner.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.roadrunner.app.R
import com.roadrunner.app.data.local.TokenStorage
import com.roadrunner.app.data.remote.AuthApiService
import com.roadrunner.app.data.remote.dto.GoogleSignInRequest
import com.roadrunner.app.data.remote.dto.LoginRequest
import com.roadrunner.app.data.remote.dto.LogoutRequest
import com.roadrunner.app.data.remote.dto.RegisterRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage,
    @ApplicationContext private val context: Context,
) {
    fun isLoggedIn(): Boolean = tokenStorage.getAccessToken() != null

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()!!.let { tokenStorage.saveTokens(it.accessToken, it.refreshToken) }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.register(RegisterRequest(email, password))
            if (response.isSuccessful) {
                response.body()!!.let { tokenStorage.saveTokens(it.accessToken, it.refreshToken) }
                Result.success(Unit)
            } else {
                val code = response.code()
                Result.failure(Exception(if (code == 409) "Email already in use" else "Registration failed"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Google Sign-In via CredentialManager — caller provides the Activity context */
    suspend fun loginWithGoogle(activityContext: Context): Result<Unit> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activityContext.getString(R.string.google_server_client_id))
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val response = authApiService.googleSignIn(GoogleSignInRequest(idToken))
                if (response.isSuccessful) {
                    response.body()!!.let { tokenStorage.saveTokens(it.accessToken, it.refreshToken) }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Google sign-in rejected by server"))
                }
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken != null) {
                authApiService.logout(LogoutRequest(refreshToken)) // Best-effort
            }
            tokenStorage.clearTokens()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenStorage.clearTokens() // Clear tokens even if network call fails
            Result.success(Unit)
        }
    }
}
