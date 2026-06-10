package com.contactsaver.autocontactsaver.plan

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Helper class for checking subscription status
 */
object SubscriptionHelper {

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_ACTIVE_PLAN = "active_plan"
    private const val KEY_PURCHASE_TIME = "purchase_time"
    private const val KEY_IS_PREMIUM = "is_premium"

    /**
     * Check if user has active premium subscription
     */
    fun isPremiumUser(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        
        if (!isPremium) return false

        val planName = prefs.getString(KEY_ACTIVE_PLAN, null) ?: return false
        val planType = try {
            PlanType.valueOf(planName)
        } catch (e: Exception) {
            return false
        }

        // If lifetime, always return true
        if (planType.isLifetime()) {
            return true
        }

        // Check if subscription is still valid
        val purchaseTime = prefs.getLong(KEY_PURCHASE_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(currentTime - purchaseTime)

        return daysPassed < planType.durationDays
    }

    /**
     * Get active plan type
     */
    fun getActivePlan(context: Context): PlanType? {
        if (!isPremiumUser(context)) return null
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val planName = prefs.getString(KEY_ACTIVE_PLAN, null) ?: return null
        
        return try {
            PlanType.valueOf(planName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get days remaining in subscription
     */
    fun getDaysRemaining(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val planName = prefs.getString(KEY_ACTIVE_PLAN, null) ?: return 0
        
        val planType = try {
            PlanType.valueOf(planName)
        } catch (e: Exception) {
            return 0
        }

        // If lifetime, return -1
        if (planType.isLifetime()) {
            return -1
        }

        val purchaseTime = prefs.getLong(KEY_PURCHASE_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(currentTime - purchaseTime)
        val daysRemaining = planType.durationDays - daysPassed.toInt()

        return if (daysRemaining > 0) daysRemaining else 0
    }

    /**
     * Clear subscription data (for testing or logout)
     */
    fun clearSubscription(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Get subscription status message
     */
    fun getSubscriptionStatusMessage(context: Context): String {
        if (!isPremiumUser(context)) {
            return "No active subscription"
        }

        val planType = getActivePlan(context)
        val daysRemaining = getDaysRemaining(context)

        return when {
            planType?.isLifetime() == true -> "Lifetime Premium Active"
            daysRemaining > 0 -> "${planType?.planName} - $daysRemaining days remaining"
            else -> "Subscription expired"
        }
    }
}
