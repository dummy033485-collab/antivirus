package com.safeshield.app.di

import android.content.Context
import androidx.room.Room
import com.safeshield.app.BuildConfig
import com.safeshield.app.core.Constants
import com.safeshield.app.core.DefaultDispatcherProvider
import com.safeshield.app.core.DispatcherProvider
import com.safeshield.app.data.local.SafeShieldDatabase
import com.safeshield.app.data.local.dao.LinkCheckDao
import com.safeshield.app.data.local.dao.LockedAppDao
import com.safeshield.app.data.local.dao.ScanHistoryDao
import com.safeshield.app.data.local.dao.SignatureDao
import com.safeshield.app.data.local.dao.WhitelistDao
import com.safeshield.app.data.remote.api.SafeBrowsingApi
import com.safeshield.app.data.remote.api.SignatureFeedApi
import com.safeshield.app.data.remote.api.VirusTotalApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class VtRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SbRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class FeedRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    /** Most repositories only need IO; inject it directly to keep signatures short. */
    @Provides @Singleton
    fun provideIoDispatcher(provider: DispatcherProvider): CoroutineDispatcher = provider.io

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SafeShieldDatabase =
        Room.databaseBuilder(context, SafeShieldDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSignatureDao(db: SafeShieldDatabase): SignatureDao = db.signatureDao()
    @Provides fun provideScanHistoryDao(db: SafeShieldDatabase): ScanHistoryDao = db.scanHistoryDao()
    @Provides fun provideWhitelistDao(db: SafeShieldDatabase): WhitelistDao = db.whitelistDao()
    @Provides fun provideLockedAppDao(db: SafeShieldDatabase): LockedAppDao = db.lockedAppDao()
    @Provides fun provideLinkCheckDao(db: SafeShieldDatabase): LinkCheckDao = db.linkCheckDao()

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
            }
        }
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideVirusTotalApi(client: OkHttpClient, moshi: Moshi): VirusTotalApi =
        retrofit(Constants.VIRUSTOTAL_BASE_URL, client, moshi).create(VirusTotalApi::class.java)

    @Provides @Singleton
    fun provideSafeBrowsingApi(client: OkHttpClient, moshi: Moshi): SafeBrowsingApi =
        retrofit(Constants.SAFE_BROWSING_BASE_URL, client, moshi).create(SafeBrowsingApi::class.java)

    @Provides @Singleton
    fun provideSignatureFeedApi(client: OkHttpClient, moshi: Moshi): SignatureFeedApi =
        retrofit("https://localhost/", client, moshi).create(SignatureFeedApi::class.java)
}
