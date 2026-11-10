package com.contactsaver.autocontactsaver.plan

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val onPurchaseCompleted: (PlanType) -> Unit,
    private val onPurchaseFailed: (String) -> Unit
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private val productList = mutableListOf<ProductDetails>()

    companion object {
        private const val TAG = "BillingManager"
    }

    
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup successful")
                    queryProducts()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                
            }
        })
    }

    
    private fun queryProducts() {
        val productIds = PlanType.values().map { it.productId }
        
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productList.clear()
                productList.addAll(productDetailsList)
                Log.d(TAG, "Products loaded: ${productList.size}")
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    
    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    
    fun launchPurchaseFlow(activity: Activity, planType: PlanType) {
        val productDetails = productList.find { it.productId == planType.productId }
        
        if (productDetails == null) {
            onPurchaseFailed("Product not found. Please try again.")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseFailed("Purchase cancelled")
        } else {
            onPurchaseFailed("Purchase failed: ${billingResult.debugMessage}")
        }
    }

    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }

            
            val planType = PlanType.values().find { 
                purchase.products.contains(it.productId) 
            }
            
            if (planType != null) {
                onPurchaseCompleted(planType)
            }
        }
    }

    
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            }
        }
    }

    
    suspend fun hasActivePurchase(): Boolean = withContext(Dispatchers.IO) {
        var hasActive = false
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                hasActive = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            }
        }
        hasActive
    }

    
    fun release() {
        billingClient?.endConnection()
        billingClient = null
    }
}
