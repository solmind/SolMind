package com.solana.solmind

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AILedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}