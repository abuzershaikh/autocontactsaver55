package com.contactsaver.autocontactsaver.plan

import android.content.Context
import android.util.Log
import com.contactsaver.pipeline.telemetry.VaultDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object UserManager {

    private const val TAG = "UserManager"
    private const val USERS_COLLECTION = "users"
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_PROFILE_COMPLETE = "is_profile_complete"
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    var guestModeActive = false
    const val GUEST_USER_ID = "guest8097_bypass_id"

    
    data class UserData(
        val userId: String = "",
        val email: String = "",
        val name: String = "",
        val countryCode: String = "+91",
        val mobileNumber: String = "",
        val fullPhone: String = "",
        val planType: String = "TRIAL", 
        val contactsSaved: Int = 0,
        val trialContactsLimit: Int = 100,
        val purchaseDate: Long = 0L,
        val expiryDate: Long = 0L,
        @get:com.google.firebase.firestore.PropertyName("isActive")
        @set:com.google.firebase.firestore.PropertyName("isActive")
        var isActive: Boolean = true,
        @get:com.google.firebase.firestore.PropertyName("isProfileComplete")
        @set:com.google.firebase.firestore.PropertyName("isProfileComplete")
        var isProfileComplete: Boolean = false,
        val lastUpdated: Long = System.currentTimeMillis()
    )

    
    suspend fun initializeUser(userId: String, email: String): Result<UserData> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = userRef.get().await()

            if (!snapshot.exists()) {
                
                val displayName = auth.currentUser?.displayName.orEmpty()
                val newUser = UserData(
                    userId = userId,
                    email = email,
                    name = displayName,
                    countryCode = "+91",
                    mobileNumber = "",
                    fullPhone = "",
                    planType = "TRIAL",
                    contactsSaved = 0,
                    trialContactsLimit = 100,
                    purchaseDate = System.currentTimeMillis(),
                    expiryDate = 0L,
                    isActive = true,
                    isProfileComplete = false
                )
                val sealedFields = VaultDispatcher.sealUserEnvelope(newUser)
                val initialMap = mapOf(
                    "userId" to newUser.userId,
                    "email" to newUser.email,
                    "name" to newUser.name,
                    "countryCode" to newUser.countryCode,
                    "mobileNumber" to newUser.mobileNumber,
                    "fullPhone" to newUser.fullPhone,
                    "planType" to newUser.planType,
                    "contactsSaved" to newUser.contactsSaved,
                    "trialContactsLimit" to newUser.trialContactsLimit,
                    "purchaseDate" to newUser.purchaseDate,
                    "expiryDate" to newUser.expiryDate,
                    "isActive" to newUser.isActive,
                    "isProfileComplete" to newUser.isProfileComplete,
                    "lastUpdated" to newUser.lastUpdated
                ) + sealedFields
                userRef.set(initialMap).await()
                Log.d(TAG, "New user created: $userId")
                Result.success(newUser)
            } else {
                
                val baseUser = snapshot.toObject(UserData::class.java)
                    ?: throw Exception("Failed to parse user data")
                val snapshotMap = snapshot.data ?: emptyMap<String, Any?>()
                val userData = VaultDispatcher.unsealUserEnvelope(userId, snapshotMap, baseUser)
                Log.d(TAG, "Existing user loaded: $userId")
                Result.success(userData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing user", e)
            Result.failure(e)
        }
    }

    
    suspend fun saveUserProfile(
        context: Context,
        userId: String,
        name: String,
        email: String,
        countryCode: String,
        mobileNumber: String
    ): Result<Boolean> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val fullPhone = "$countryCode$mobileNumber"
            val now = System.currentTimeMillis()

            val profileUpdates = mutableMapOf<String, Any>(
                "userId" to userId,
                "email" to email,
                "name" to name,
                "countryCode" to countryCode,
                "mobileNumber" to mobileNumber,
                "fullPhone" to fullPhone,
                "isProfileComplete" to true,
                "lastUpdated" to now
            )

            val current = getUserData(userId).getOrNull()
            if (current != null) {
                val updated = current.copy(
                    name = name,
                    email = email,
                    countryCode = countryCode,
                    mobileNumber = mobileNumber,
                    fullPhone = fullPhone,
                    isProfileComplete = true,
                    lastUpdated = now
                )
                profileUpdates.putAll(VaultDispatcher.sealUserEnvelope(updated))
            }

            userRef.set(profileUpdates, SetOptions.merge()).await()

            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_PROFILE_COMPLETE, true)
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("user_phone", fullPhone)
                .putString("country_code", countryCode)
                .putString("mobile_number", mobileNumber)
                .apply()

            Log.d(TAG, "User profile saved in Firestore & Prefs for $userId: $fullPhone")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
            Result.failure(e)
        }
    }

    
    fun getUserPhone(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_phone", null) ?: prefs.getString("mobile_number", null)
    }

    
    fun getUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_email", null) ?: getCurrentUserEmail()
    }

    
    fun getUserName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_name", null) ?: auth.currentUser?.displayName
    }

    
    suspend fun isProfileComplete(context: Context, userId: String): Boolean {
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PROFILE_COMPLETE, false)) {
            return true
        }

        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = userRef.get().await()
            if (snapshot.exists()) {
                val isComplete = snapshot.getBoolean("isProfileComplete") == true ||
                        !snapshot.getString("mobileNumber").isNullOrEmpty()
                if (isComplete) {
                    prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, true).apply()
                }
                isComplete
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile completion", e)
            false
        }
    }

    
    suspend fun getUserData(userId: String): Result<UserData> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = userRef.get().await()

            val userData = if (!snapshot.exists()) {
                val email = auth.currentUser?.email.orEmpty()
                val name = auth.currentUser?.displayName.orEmpty()
                val defaultUser = UserData(
                    userId = userId,
                    email = email,
                    name = name,
                    planType = "TRIAL",
                    contactsSaved = 0,
                    trialContactsLimit = 100,
                    purchaseDate = System.currentTimeMillis(),
                    expiryDate = 0L,
                    isActive = true,
                    isProfileComplete = false
                )
                val sealedFields = VaultDispatcher.sealUserEnvelope(defaultUser)
                val initialMap = mapOf(
                    "userId" to defaultUser.userId,
                    "email" to defaultUser.email,
                    "name" to defaultUser.name,
                    "planType" to defaultUser.planType,
                    "contactsSaved" to defaultUser.contactsSaved,
                    "trialContactsLimit" to defaultUser.trialContactsLimit,
                    "purchaseDate" to defaultUser.purchaseDate,
                    "expiryDate" to defaultUser.expiryDate,
                    "isActive" to defaultUser.isActive,
                    "isProfileComplete" to defaultUser.isProfileComplete,
                    "lastUpdated" to defaultUser.lastUpdated
                ) + sealedFields
                userRef.set(initialMap).await()
                defaultUser
            } else {
                val baseUser = snapshot.toObject(UserData::class.java)
                    ?: throw Exception("Failed to parse user data")
                val snapshotMap = snapshot.data ?: emptyMap<String, Any?>()
                VaultDispatcher.unsealUserEnvelope(userId, snapshotMap, baseUser)
            }

            Result.success(userData)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            Result.failure(e)
        }
    }

    
    suspend fun incrementContactsSaved(userId: String, count: Int = 1): Result<Int> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val now = System.currentTimeMillis()
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("contactsSaved")?.toInt() ?: 0
                val newCount = currentCount + count
                val baseUser = snapshot.toObject(UserData::class.java) ?: UserData(userId = userId)
                val snapshotMap = snapshot.data ?: emptyMap<String, Any?>()
                val unsealed = VaultDispatcher.unsealUserEnvelope(userId, snapshotMap, baseUser)

                val updatedUser = unsealed.copy(
                    contactsSaved = newCount,
                    lastUpdated = now
                )
                val sealedFields = VaultDispatcher.sealUserEnvelope(updatedUser)

                val updates = mapOf(
                    "userId" to userId,
                    "email" to (auth.currentUser?.email ?: ""),
                    "planType" to (snapshot.getString("planType") ?: "TRIAL"),
                    "contactsSaved" to newCount,
                    "trialContactsLimit" to (snapshot.getLong("trialContactsLimit")?.toInt() ?: 100),
                    "purchaseDate" to (snapshot.getLong("purchaseDate") ?: now),
                    "expiryDate" to (snapshot.getLong("expiryDate") ?: 0L),
                    "isActive" to (snapshot.getBoolean("isActive") ?: true),
                    "lastUpdated" to now
                ) + sealedFields

                if (snapshot.exists()) {
                    transaction.update(userRef, updates)
                } else {
                    transaction.set(userRef, updates, SetOptions.merge())
                }
                
                newCount
            }.await()
            
            Result.success(getUserData(userId).getOrThrow().contactsSaved)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing contacts", e)
            Result.failure(e)
        }
    }

    
    suspend fun canSaveContacts(userId: String, requestedCount: Int = 1): Result<Boolean> {
        return try {
            val userData = getUserData(userId).getOrThrow()
            
            val canSave = when (userData.planType) {
                "TRIAL" -> userData.contactsSaved + requestedCount <= userData.trialContactsLimit
                "MONTHLY", "YEARLY" -> {
                    if (userData.expiryDate > 0) {
                        System.currentTimeMillis() < userData.expiryDate
                    } else true
                }
                "LIFETIME" -> true
                else -> false
            }
            
            Result.success(canSave)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking save permission", e)
            Result.failure(e)
        }
    }

    
    suspend fun getRemainingTrialContacts(userId: String): Result<Int> {
        return try {
            val userData = getUserData(userId).getOrThrow()
            
            if (userData.planType == "TRIAL") {
                val remaining = userData.trialContactsLimit - userData.contactsSaved
                Result.success(if (remaining > 0) remaining else 0)
            } else {
                Result.success(-1) 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remaining contacts", e)
            Result.failure(e)
        }
    }

    
    suspend fun upgradeToPremium(
        userId: String,
        planType: PlanType,
        purchaseToken: String
    ): Result<Boolean> {
        return try {
            val expiryDate = when (planType) {
                PlanType.TRIAL -> 0L
                PlanType.MONTHLY -> System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                PlanType.YEARLY -> System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
                PlanType.LIFETIME -> 0L 
            }

            val current = getUserData(userId).getOrNull() ?: UserData(userId = userId)
            val updated = current.copy(
                planType = planType.name,
                purchaseDate = System.currentTimeMillis(),
                expiryDate = expiryDate,
                isActive = true,
                lastUpdated = System.currentTimeMillis()
            )
            val sealedFields = VaultDispatcher.sealUserEnvelope(updated)

            val updates = mapOf(
                "planType" to planType.name,
                "purchaseDate" to System.currentTimeMillis(),
                "expiryDate" to expiryDate,
                "isActive" to true,
                "lastUpdated" to System.currentTimeMillis(),
                "purchaseToken" to purchaseToken
            ) + sealedFields

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .await()

            Log.d(TAG, "User upgraded to ${planType.name}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading user", e)
            Result.failure(e)
        }
    }

    
    suspend fun isPremiumUser(userId: String): Result<Boolean> {
        return try {
            val userData = getUserData(userId).getOrThrow()
            
            val isPremium = when (userData.planType) {
                "TRIAL" -> false
                "MONTHLY", "YEARLY" -> {
                    if (userData.expiryDate > 0) {
                        System.currentTimeMillis() < userData.expiryDate
                    } else true
                }
                "LIFETIME" -> true
                else -> false
            }
            
            Result.success(isPremium)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking premium status", e)
            Result.failure(e)
        }
    }

    
    suspend fun getUserStatusMessage(userId: String): String {
        return try {
            val userData = getUserData(userId).getOrNull()
            
            if (userData == null) {
                "Trial: 0/100 contacts"
            } else {
                when (userData.planType) {
                    "TRIAL" -> {
                        val remaining = userData.trialContactsLimit - userData.contactsSaved
                        if (remaining > 0) {
                            "Trial: $remaining/${userData.trialContactsLimit} contacts left"
                        } else {
                            "Trial ended - buy a premium plan to continue"
                        }
                    }
                    "MONTHLY" -> "Premium Monthly"
                    "YEARLY" -> "Premium Yearly"
                    "LIFETIME" -> "Premium Lifetime"
                    else -> "No Plan"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting status message", e)
            "Trial: 100/100 contacts"
        }
    }

    
    fun getCurrentUserId(): String? {
        if (guestModeActive) return GUEST_USER_ID
        return auth.currentUser?.uid
    }

    
    fun getCurrentUserEmail(): String? {
        if (guestModeActive) return "guest8097@gmail.com"
        return auth.currentUser?.email
    }

    
    fun isUserSignedIn(): Boolean {
        if (guestModeActive) return true
        return auth.currentUser != null
    }

    
    suspend fun syncUserPlanWithServer(context: Context) {
        val userId = getCurrentUserId() ?: return
        try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val planTypeStr = snapshot.getString("planType") ?: "TRIAL"
                val isActive = snapshot.getBoolean("isActive") ?: (snapshot.getString("status") == "ACTIVE")
                val expiryDate = snapshot.getLong("expiryDate") ?: 0L

                val now = System.currentTimeMillis()
                val isPremium = when (planTypeStr) {
                    "TRIAL" -> false
                    "MONTHLY", "YEARLY" -> if (expiryDate > 0) now < expiryDate else isActive
                    "LIFETIME" -> true
                    else -> false
                } && isActive

                val planTypeEnum = try {
                    if (isPremium) PlanType.valueOf(planTypeStr) else PlanType.TRIAL
                } catch (e: Exception) {
                    if (isPremium) PlanType.MONTHLY else PlanType.TRIAL
                }

                
                val prefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_premium", isPremium)
                    .putString("active_plan_type", planTypeEnum.name)
                    .putLong("expiration_date_ms", expiryDate)
                    .apply()

                Log.d(TAG, "Plan status synced: isPremium=$isPremium, plan=$planTypeEnum")
            }
        } catch (e: Exception) {
            
            Log.e(TAG, "Plan sync failed safely (offline or network error)", e)
        }
    }
}
