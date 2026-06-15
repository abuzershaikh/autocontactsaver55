package com.contactsaver.autocontactsaver.plan

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Manager for handling user subscription and trial status with Firestore
 */
object UserManager {

    private const val TAG = "UserManager"
    private const val USERS_COLLECTION = "users"
    private const val PREFS_NAME = "user_prefs"
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * User data model
     */
    data class UserData(
        val userId: String = "",
        val email: String = "",
        val planType: String = "TRIAL", // TRIAL, MONTHLY, YEARLY, LIFETIME
        val contactsSaved: Int = 0,
        val trialContactsLimit: Int = 100,
        val purchaseDate: Long = 0L,
        val expiryDate: Long = 0L,
        val isActive: Boolean = true,
        val lastUpdated: Long = System.currentTimeMillis()
    )

    /**
     * Initialize user in Firestore (called after sign-in)
     */
    suspend fun initializeUser(userId: String, email: String): Result<UserData> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = userRef.get().await()

            if (!snapshot.exists()) {
                // New user - create with trial
                val newUser = UserData(
                    userId = userId,
                    email = email,
                    planType = "TRIAL",
                    contactsSaved = 0,
                    trialContactsLimit = 100,
                    purchaseDate = System.currentTimeMillis(),
                    expiryDate = 0L,
                    isActive = true
                )
                userRef.set(newUser).await()
                Log.d(TAG, "New user created: $userId")
                Result.success(newUser)
            } else {
                // Existing user - fetch data
                val userData = snapshot.toObject(UserData::class.java)
                    ?: throw Exception("Failed to parse user data")
                Log.d(TAG, "Existing user loaded: $userId")
                Result.success(userData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing user", e)
            Result.failure(e)
        }
    }

    /**
     * Get user data from Firestore
     */
    suspend fun getUserData(userId: String): Result<UserData> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = userRef.get().await()

            val userData = if (!snapshot.exists()) {
                val email = auth.currentUser?.email.orEmpty()
                val defaultUser = UserData(
                    userId = userId,
                    email = email,
                    planType = "TRIAL",
                    contactsSaved = 0,
                    trialContactsLimit = 100,
                    purchaseDate = System.currentTimeMillis(),
                    expiryDate = 0L,
                    isActive = true
                )
                userRef.set(defaultUser).await()
                defaultUser
            } else {
                snapshot.toObject(UserData::class.java)
                    ?: throw Exception("Failed to parse user data")
            }

            Result.success(userData)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            Result.failure(e)
        }
    }

    /**
     * Increment contacts saved count
     */
    suspend fun incrementContactsSaved(userId: String, count: Int = 1): Result<Int> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val now = System.currentTimeMillis()
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("contactsSaved")?.toInt() ?: 0
                val newCount = currentCount + count

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
                )

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

    /**
     * Check if user can save contacts (within trial limit or has premium)
     */
    suspend fun canSaveContacts(userId: String, requestedCount: Int = 1): Result<Boolean> {
        return try {
            val userData = getUserData(userId).getOrThrow()
            
            val canSave = when (userData.planType) {
                "TRIAL" -> userData.contactsSaved + requestedCount <= userData.trialContactsLimit
                "MONTHLY", "YEARLY" -> {
                    // Check if plan is still valid
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

    /**
     * Get remaining trial contacts
     */
    suspend fun getRemainingTrialContacts(userId: String): Result<Int> {
        return try {
            val userData = getUserData(userId).getOrThrow()
            
            if (userData.planType == "TRIAL") {
                val remaining = userData.trialContactsLimit - userData.contactsSaved
                Result.success(if (remaining > 0) remaining else 0)
            } else {
                Result.success(-1) // -1 means unlimited (premium)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remaining contacts", e)
            Result.failure(e)
        }
    }

    /**
     * Upgrade user to premium plan
     */
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
                PlanType.LIFETIME -> 0L // 0 means lifetime
            }

            val updates = mapOf(
                "planType" to planType.name,
                "purchaseDate" to System.currentTimeMillis(),
                "expiryDate" to expiryDate,
                "isActive" to true,
                "lastUpdated" to System.currentTimeMillis(),
                "purchaseToken" to purchaseToken
            )

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

    /**
     * Check if user has premium plan
     */
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

    /**
     * Get user status message for UI display
     */
    suspend fun getUserStatusMessage(userId: String): String {
        return try {
            val userData = getUserData(userId).getOrNull()
            
            if (userData == null) {
                "Trial: 0/100 contacts" // Default if no data yet
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
            "Trial: 100/100 contacts" // Safe fallback
        }
    }

    /**
     * Get current user ID from Firebase Auth
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Check if user is signed in
     */
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }
}
