package io.github.anshireminder.app

import android.app.Application

class AnshiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }
}
