package com.solana.solmind.di

import com.solana.solmind.service.HuggingFaceApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val HUGGING_FACE_BASE_URL = "https://huggingface.co/"
    
    @Provides
    @Singleton
    @Named("huggingface")
    fun provideHuggingFaceOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("huggingface")
    fun provideHuggingFaceRetrofit(
        @Named("huggingface") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(HUGGING_FACE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideHuggingFaceApiService(
        @Named("huggingface") retrofit: Retrofit
    ): HuggingFaceApiService {
        return retrofit.create(HuggingFaceApiService::class.java)
    }
}