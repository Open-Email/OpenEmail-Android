package com.mercata.pingworks

import android.app.Application
import androidx.room.Room
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.utils.BioManager
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.SharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class MainApplication : Application() {

    private val appModule = module {
        single { SharedPreferences(get()) }
        single { BioManager(get()) }
        single { Downloader(get()) }
        single {
            Room.databaseBuilder(
                get(),
                AppDatabase::class.java, "ping-works"
            ).build()
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }


}