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
import com.mercata.openemail.repository.LogoutRepository
import com.mercata.openemail.repository.ProcessIncomingIntentsRepository
import com.mercata.openemail.repository.SyncRepository
import com.mercata.openemail.repository.UserDataUpdateRepository
import com.mercata.openemail.utils.BioManager
import com.mercata.openemail.utils.CopyAttachmentService
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.ReplyBodyConstructor
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.SoundPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module


class MainApplication : Application(), ImageLoaderFactory {

    private val appModule = module {
        single { SharedPreferences(get(), get()) }
        single { DownloadRepository(get(), get()) }
        single { UserDataUpdateRepository(get()) }
        factory { BioManager(get()) }
        factory { ReplyBodyConstructor(get()) }
        factory { FileUtils(get()) }
        factory { SoundPlayer(get()) }
        factory { CopyAttachmentService(get()) }
        single { ProcessIncomingIntentsRepository(get(), get()) }
        single { SyncRepository(get(), get(), get()) }
        single { AddContactRepository(get(), get(), get()) }
        single { LogoutRepository(get(), get(), get()) }
        single {
            Room.databaseBuilder(
                get(),
                AppDatabase::class.java, "ping-works"
            )
                .fallbackToDestructiveMigration()
                .build()
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
        return ImageLoader(this).newBuilder()
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
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