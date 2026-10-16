package com.camon.launcher

import android.app.Application
import com.camon.launcher.core.LauncherRepository

class CamonApp : Application() {

    lateinit var repository: LauncherRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = LauncherRepository(applicationContext)
    }
}
