package com.contactsaver.autocontactsaver.plan

import android.content.Context
import java.util.concurrent.TimeUnit

object SubscriptionHelper {

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_ACTIVE_PLAN = "active_plan"
    private const val KEY_PURCHASE_TIME = "purchase_time"
    private const val KEY_IS_PREMIUM = "is_premium"

    
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

        
        if (planType.isLifetime()) {
            return true
        }

        
        val purchaseTime = prefs.getLong(KEY_PURCHASE_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(currentTime - purchaseTime)

        return daysPassed < planType.durationDays
    }

    
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

    
    fun getDaysRemaining(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val planName = prefs.getString(KEY_ACTIVE_PLAN, null) ?: return 0
        
        val planType = try {
            PlanType.valueOf(planName)
        } catch (e: Exception) {
            return 0
        }

        
        if (planType.isLifetime()) {
            return -1
        }

        val purchaseTime = prefs.getLong(KEY_PURCHASE_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(currentTime - purchaseTime)
        val daysRemaining = planType.durationDays - daysPassed.toInt()

        return if (daysRemaining > 0) daysRemaining else 0
    }

    
    fun clearSubscription(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    
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
