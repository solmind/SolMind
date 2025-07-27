package com.solmind

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SolMindApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager with Hilt
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}