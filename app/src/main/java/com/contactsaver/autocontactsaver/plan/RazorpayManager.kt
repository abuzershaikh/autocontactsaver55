package com.contactsaver.autocontactsaver.plan

import android.app.Activity
import android.content.Context
import android.util.Log
import com.razorpay.Checkout
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object RazorpayManager {

    private const val TAG = "RazorpayManager"
    
    
    const val KEY_ID = "rzp_live_TI2wdFKYDJdAxY"
    const val KEY_SECRET = "0pNQOQBRWxmtdE8mPVLlvYfi"
    const val WORKER_URL = "https://razorpay-worker.zestbizar.workers.dev"

    
    fun initialize(context: Context) {
        try {
            Checkout.preload(context.applicationContext)
            Log.d(TAG, "Razorpay SDK preloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading Razorpay: ${e.message}")
        }
    }

    
    fun startPaymentFlow(
        activity: Activity,
        planType: PlanType,
        userEmail: String? = null,
        userPhone: String? = null
    ) {
        val checkout = Checkout()
        checkout.setKeyID(KEY_ID)

        try {
            val options = JSONObject()
            options.put("name", "Auto Contacts Saver")
            options.put("description", "${planType.planName} (${planType.price})")
            options.put("currency", "INR")

            
            val amountInPaise = when (planType) {
                PlanType.MONTHLY -> 19900   
                PlanType.YEARLY -> 99900    
                PlanType.LIFETIME -> 199900 
                PlanType.TRIAL -> 0
            }
            options.put("amount", amountInPaise)

            
            val prefill = JSONObject()
            if (!userEmail.isNullOrEmpty()) {
                prefill.put("email", userEmail)
            }
            if (!userPhone.isNullOrEmpty()) {
                prefill.put("contact", userPhone)
            }
            options.put("prefill", prefill)

            
            val theme = JSONObject()
            theme.put("color", "#7C4DFF") 
            options.put("theme", theme)

            
            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 2)
            options.put("retry", retryObj)

            
            checkout.open(activity, options)
            Log.d(TAG, "Razorpay checkout opened for ${planType.planName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Razorpay payment: ${e.message}", e)
            throw e
        }
    }

    
    fun verifySignatureLocally(orderId: String?, paymentId: String?, signature: String?): Boolean {
        if (signature.isNullOrEmpty() || paymentId.isNullOrEmpty()) return false

        try {
            
            val dataToVerify = if (!orderId.isNullOrEmpty()) {
                "$orderId|$paymentId"
            } else {
                paymentId
            }

            val secretKeySpec = SecretKeySpec(KEY_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKeySpec)

            val hashBytes = mac.doFinal(dataToVerify.toByteArray(Charsets.UTF_8))
            val generatedSignature = hashBytes.joinToString("") { "%02x".format(it) }

            val isValid = generatedSignature.equals(signature, ignoreCase = true)
            Log.d(TAG, "Signature verification result: $isValid")
            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification exception: ${e.message}", e)
            return false
        }
    }
}
