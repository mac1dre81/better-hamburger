package com.dredio.textraocr

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

data class PremiumUiState(
    val isPremium: Boolean = false,
    val subscriptionAvailable: Boolean = false,
    val availableProducts: List<ProductDetails> = emptyList(),
)

class PremiumBillingManager(
    private val context: Context,
    private val onStateChanged: (PremiumUiState) -> Unit,
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private var premiumState = PremiumUiState(
        isPremium = AppSettings.isPremiumCached(context),
        subscriptionAvailable = false,
        availableProducts = emptyList()
    )
    private var cachedProducts: List<ProductDetails> = emptyList()

    fun start() {
        onStateChanged(premiumState)

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                publishState(premiumState.copy(subscriptionAvailable = false))
            }

            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    publishState(premiumState.copy(subscriptionAvailable = false))
                    return
                }

                queryPurchases()
                queryProductDetails()
            }
        })
    }

    fun launchSubscription(activity: Activity, details: ProductDetails): Boolean {
        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return false

        val billingParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, billingParams)
        return true
    }

    fun stop() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: com.android.billingclient.api.BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else {
            queryPurchases()
        }
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_WEEKLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_YEARLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            val details = result.productDetailsList.orEmpty()
            val available = billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                details.isNotEmpty()
            cachedProducts = details
            publishState(premiumState.copy(
                subscriptionAvailable = available,
                availableProducts = details
            ))
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            } else {
                publishState(premiumState.copy(isPremium = AppSettings.isPremiumCached(context)))
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val premiumPurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { productId ->
                    productId == PREMIUM_WEEKLY_PRODUCT_ID ||
                        productId == PREMIUM_MONTHLY_PRODUCT_ID ||
                        productId == PREMIUM_YEARLY_PRODUCT_ID
                }
        }

        premiumPurchase?.takeIf { !it.isAcknowledged }?.let { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }

        val isPremium = premiumPurchase != null
        AppSettings.setPremiumCached(context, isPremium)
        publishState(premiumState.copy(isPremium = isPremium))
    }

    private fun publishState(state: PremiumUiState) {
        premiumState = state
        onStateChanged(state)
    }

    companion object {
        private const val PREMIUM_WEEKLY_PRODUCT_ID = "textraocr_premium_weekly"
        private const val PREMIUM_MONTHLY_PRODUCT_ID = "textraocr_premium_monthly"
        private const val PREMIUM_YEARLY_PRODUCT_ID = "textraocr_premium_yearly"
    }
}
