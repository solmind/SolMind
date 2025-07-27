package com.solmind.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solmind.repository.LedgerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Date

@HiltWorker
class WalletSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LedgerRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "WalletSyncWorker"
        const val WORK_NAME = "wallet_sync_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting automatic wallet sync")
            
            // Get all active wallets
            val activeWallets = repository.getActiveWallets().first()
            
            if (activeWallets.isEmpty()) {
                Log.d(TAG, "No active wallets to sync")
                return Result.success()
            }
            
            var successCount = 0
            var errorCount = 0
            
            // Sync each wallet
            activeWallets.forEach { wallet ->
                try {
                    Log.d(TAG, "Syncing wallet: ${wallet.address}")
                    repository.syncWalletTransactions(wallet.address)
                    repository.updateWalletSyncTime(wallet.address, Date())
                    successCount++
                    Log.d(TAG, "Successfully synced wallet: ${wallet.address}")
                } catch (e: IllegalStateException) {
                    // Handle model availability issues
                    if (e.message?.contains("No model selected") == true || 
                        e.message?.contains("not downloaded") == true) {
                        Log.w(TAG, "Sync failed for wallet ${wallet.address}: AI model not available. ${e.message}")
                        // Continue with other wallets, but log this as a warning rather than error
                        // The sync will work but without AI categorization
                    } else {
                        Log.e(TAG, "Failed to sync wallet ${wallet.address}: ${e.message}", e)
                        errorCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync wallet ${wallet.address}: ${e.message}", e)
                    errorCount++
                }
            }
            
            Log.d(TAG, "Wallet sync completed. Success: $successCount, Errors: $errorCount")
            
            // Return success if at least one wallet synced successfully
            if (successCount > 0) {
                Result.success()
            } else if (errorCount > 0) {
                // Retry if all wallets failed
                Result.retry()
            } else {
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Wallet sync worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}