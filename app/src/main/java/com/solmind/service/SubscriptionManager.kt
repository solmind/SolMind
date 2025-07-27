package com.solmind.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
    
    private val _isSubscribed = MutableStateFlow(getSubscriptionStatus())
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()
    
    private val _subscriptionTier = MutableStateFlow(getSubscriptionTier())
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()
    
    companion object {
        private const val KEY_IS_SUBSCRIBED = "is_subscribed"
        private const val KEY_SUBSCRIPTION_TIER = "subscription_tier"
        private const val KEY_SUBSCRIPTION_DATE = "subscription_date"
    }
    
    private fun getSubscriptionStatus(): Boolean {
        return prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
    }
    
    private fun getSubscriptionTier(): SubscriptionTier {
        val tierName = prefs.getString(KEY_SUBSCRIPTION_TIER, SubscriptionTier.FREE.name)
        return try {
            SubscriptionTier.valueOf(tierName ?: SubscriptionTier.FREE.name)
        } catch (e: IllegalArgumentException) {
            SubscriptionTier.FREE
        }
    }
    
    fun upgradeToMaster() {
        prefs.edit()
            .putBoolean(KEY_IS_SUBSCRIBED, true)
            .putString(KEY_SUBSCRIPTION_TIER, SubscriptionTier.MASTER.name)
            .putLong(KEY_SUBSCRIPTION_DATE, System.currentTimeMillis())
            .apply()
        
        _isSubscribed.value = true
        _subscriptionTier.value = SubscriptionTier.MASTER
    }
    
    fun cancelSubscription() {
        prefs.edit()
            .putBoolean(KEY_IS_SUBSCRIBED, false)
            .putString(KEY_SUBSCRIPTION_TIER, SubscriptionTier.FREE.name)
            .apply()
        
        _isSubscribed.value = false
        _subscriptionTier.value = SubscriptionTier.FREE
    }
    
    fun hasCloudAccess(): Boolean {
        return _subscriptionTier.value == SubscriptionTier.MASTER
    }
    
    fun getSubscriptionDate(): Long {
        return prefs.getLong(KEY_SUBSCRIPTION_DATE, 0L)
    }
}

enum class SubscriptionTier {
    FREE,
    MASTER
}

data class SubscriptionBenefit(
    val title: String,
    val description: String,
    val icon: String
)

object SubscriptionBenefits {
    val masterBenefits = listOf(
        SubscriptionBenefit(
            title = "Cloud AI Models",
            description = "Access to powerful cloud-based AI models for enhanced transaction parsing",
            icon = "cloud"
        ),
        SubscriptionBenefit(
            title = "Support Development",
            description = "Help us continue improving SolMind with new features and updates",
            icon = "favorite"
        ),
        SubscriptionBenefit(
            title = "Pro Features",
            description = "Get early access to advanced features and premium functionality",
            icon = "star"
        ),
        SubscriptionBenefit(
            title = "Priority Support",
            description = "Receive priority customer support and faster response times",
            icon = "support"
        ),
        SubscriptionBenefit(
            title = "Advanced Analytics",
            description = "Detailed spending insights and advanced financial analytics",
            icon = "analytics"
        )
    )
}