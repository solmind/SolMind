package com.solana.solmind.manager

import android.content.Context
import androidx.work.*
import com.solana.solmind.worker.WalletSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    private val _isAutoSyncEnabled = MutableStateFlow(true)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()
    
    private val _syncInterval = MutableStateFlow(SyncInterval.FIFTEEN_MINUTES)
    val syncInterval: StateFlow<SyncInterval> = _syncInterval.asStateFlow()
    
    enum class SyncInterval(val displayName: String, val minutes: Long) {
        FIVE_MINUTES("5 minutes", 5),
        FIFTEEN_MINUTES("15 minutes", 15),
        THIRTY_MINUTES("30 minutes", 30),
        ONE_HOUR("1 hour", 60),
        TWO_HOURS("2 hours", 120),
        SIX_HOURS("6 hours", 360)
    }
    
    fun setAutoSyncEnabled(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        if (enabled) {
            schedulePeriodicSync()
        } else {
            cancelPeriodicSync()
        }
    }
    
    fun setSyncInterval(interval: SyncInterval) {
        _syncInterval.value = interval
        if (_isAutoSyncEnabled.value) {
            schedulePeriodicSync() // Reschedule with new interval
        }
    }
    
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<WalletSyncWorker>(
            _syncInterval.value.minutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WalletSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(WalletSyncWorker.WORK_NAME)
    }
    
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<WalletSyncWorker>()
            .setConstraints(constraints)
            .build()
        
        workManager.enqueue(syncRequest)
    }
    
    fun getSyncWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(WalletSyncWorker.WORK_NAME)
}