package com.mercata.openemail

import android.app.Application
import androidx.room.Room
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.repository.AddContactRepository
import com.mercata.openemail.repository.SendMessageRepository
import com.mercata.openemail.utils.BioManager
import com.mercata.openemail.utils.CopyAttachmentService
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.SoundPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class MainApplication : Application() {

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

        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }


}