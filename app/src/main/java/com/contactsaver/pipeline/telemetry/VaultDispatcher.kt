package com.contactsaver.pipeline.telemetry

import com.contactsaver.autocontactsaver.plan.UserManager
import com.contactsaver.engine.security.EntropyCipherNode
import org.json.JSONObject

object VaultDispatcher {

    fun sealUserEnvelope(userData: UserManager.UserData): Map<String, Any> {
        val json = JSONObject().apply {
            put("userId", userData.userId)
            put("planType", userData.planType)
            put("mobileNumber", userData.mobileNumber)
            put("fullPhone", userData.fullPhone)
            put("countryCode", userData.countryCode)
            put("contactsSaved", userData.contactsSaved)
            put("trialContactsLimit", userData.trialContactsLimit)
            put("purchaseDate", userData.purchaseDate)
            put("expiryDate", userData.expiryDate)
            put("isActive", userData.isActive)
            put("isProfileComplete", userData.isProfileComplete)
            put("lastUpdated", userData.lastUpdated)
        }

        val (envelope, signature) = EntropyCipherNode.sealPayload(
            userData.userId,
            json.toString()
        )

        return mapOf(
            "sec_envelope" to envelope,
            "sec_signature" to signature,
            "sec_synced" to System.currentTimeMillis()
        )
    }

    fun unsealUserEnvelope(
        userId: String,
        snapshotMap: Map<String, Any?>,
        fallbackUser: UserManager.UserData
    ): UserManager.UserData {
        val envelope = snapshotMap["sec_envelope"] as? String
        val signature = snapshotMap["sec_signature"] as? String

        if (!envelope.isNullOrEmpty()) {
            val decryptedJson = EntropyCipherNode.unsealPayload(userId, envelope, signature)
            if (!decryptedJson.isNullOrEmpty()) {
                try {
                    val obj = JSONObject(decryptedJson)
                    return fallbackUser.copy(
                        planType = obj.optString("planType", fallbackUser.planType),
                        mobileNumber = obj.optString("mobileNumber", fallbackUser.mobileNumber),
                        fullPhone = obj.optString("fullPhone", fallbackUser.fullPhone),
                        countryCode = obj.optString("countryCode", fallbackUser.countryCode),
                        contactsSaved = obj.optInt("contactsSaved", fallbackUser.contactsSaved),
                        trialContactsLimit = obj.optInt("trialContactsLimit", fallbackUser.trialContactsLimit),
                        purchaseDate = obj.optLong("purchaseDate", fallbackUser.purchaseDate),
                        expiryDate = obj.optLong("expiryDate", fallbackUser.expiryDate),
                        isActive = obj.optBoolean("isActive", fallbackUser.isActive),
                        isProfileComplete = obj.optBoolean("isProfileComplete", fallbackUser.isProfileComplete),
                        lastUpdated = obj.optLong("lastUpdated", fallbackUser.lastUpdated)
                    )
                } catch (e: Exception) {
                    
                }
            }
        }

        
        return fallbackUser
    }
}
