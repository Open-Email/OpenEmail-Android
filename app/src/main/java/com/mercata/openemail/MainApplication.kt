@file:OptIn(ExperimentalCoilApi::class)

package com.mercata.openemail

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.repository.AddContactRepository
import com.mercata.openemail.repository.SendMessageRepository
import com.mercata.openemail.utils.BioManager
import com.mercata.openemail.utils.CopyAttachmentService
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.SoundPlayer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module


class MainApplication : Application(), ImageLoaderFactory {

    private val appModule = module {
        single { SharedPreferences(get(), get()) }
        single { DownloadRepository(get(), get()) }
        factory { BioManager(get()) }
        factory { FileUtils(get()) }
        factory { SoundPlayer(get()) }
        factory { CopyAttachmentService(get()) }
        single { SendMessageRepository(get(), get(), get(), get(), get()) }
        single { AddContactRepository(get(), get(), get()) }
        single {
            Room.databaseBuilder(
                get(),
                AppDatabase::class.java, "ping-works"
            ).build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader {
        //TODO enable caching after cloudflare cache timeout adjustment
        return ImageLoader(this).newBuilder()
            .okHttpClient {

                val interceptor = Interceptor { chain ->
                    var request: Request = chain.request()
                    val builder: Request.Builder =
                        request.newBuilder().addHeader("Cache-Control", "no-cache")
                    request = builder.build()
                    chain.proceed(request)
                }

                OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .cache(null)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.DISABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()
            }

            .diskCachePolicy(CachePolicy.DISABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizePercent(0.03)
                    .directory(cacheDir)
                    .build()
            }
            .logger(DebugLogger())
            .build()
    }

}