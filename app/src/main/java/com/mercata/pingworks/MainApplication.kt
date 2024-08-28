package com.mercata.pingworks

import android.app.Application
import com.mercata.pingworks.common.NavigationDrawerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class MainApplication : Application() {

    private val appModule = module {
        single { SharedPreferences(get()) }
        single { BioManager(get()) }
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