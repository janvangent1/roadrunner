package com.roadrunner.app.data.remote.interceptor

import com.roadrunner.app.data.local.TokenStorage
import com.roadrunner.app.data.remote.AuthApiService
import com.roadrunner.app.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApiService: AuthApiService,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null
        val newTokens = runBlocking {
            try {
                val result = authApiService.refresh(RefreshRequest(refreshToken))
                if (result.isSuccessful) result.body() else null
            } catch (e: Exception) { null }
        }
        if (newTokens == null) {
            tokenStorage.clearTokens()
            return null // Signal logout — Plan 03 NavGraph handles navigation
        }
        tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }
}
