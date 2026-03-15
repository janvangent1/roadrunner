package com.roadrunner.app.di

import com.roadrunner.app.BuildConfig
import com.roadrunner.app.data.remote.ApiService
import com.roadrunner.app.data.remote.AuthApiService
import com.roadrunner.app.data.remote.interceptor.AuthInterceptor
import com.roadrunner.app.data.remote.interceptor.TokenRefreshAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun buildCertificatePinner(): CertificatePinner? {
        if (BuildConfig.DEBUG) return null
        // Pin format: sha256/BASE64_ENCODED_SHA256
        // Hostname: replace with your Cloudflare Tunnel stable hostname before release.
        // The CERT_PIN_SHA256 buildConfigField is set in build.gradle.kts release block.
        return CertificatePinner.Builder()
            .add("*.trycloudflare.com", "sha256/${BuildConfig.CERT_PIN_SHA256}")
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApiService {
        val clientBuilder = OkHttpClient.Builder()
        buildCertificatePinner()?.let { clientBuilder.certificatePinner(it) }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(clientBuilder.build())
            .build()
            .create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenRefreshAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
        buildCertificatePinner()?.let { builder.certificatePinner(it) }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApiService(client: OkHttpClient): ApiService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
}
