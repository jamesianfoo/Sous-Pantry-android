package com.souspantry.app

import android.app.Application
import com.souspantry.app.services.Subscriptions
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SousPantryApp : Application() {

    @Inject lateinit var subscriptions: Subscriptions

    override fun onCreate() {
        super.onCreate()
        subscriptions.configure()
    }
}
