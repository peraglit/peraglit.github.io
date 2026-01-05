package com.peraglit.trucksim

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams

class BillingManager(private val activity: Activity) : PurchasesUpdatedListener {
    var onPurchaseAcknowledged: (() -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(activity)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    fun startConnection(onConnected: () -> Unit, onError: () -> Unit) {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    onError()
                }

                override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        onConnected()
                    } else {
                        onError()
                    }
                }
            }
        )
    }

    fun launchFuelPackPurchase() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("fuel_pack_basic")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingResponseCode.OK || productDetailsList.isEmpty()) {
                Log.w("Billing", "Ürün detayları alınamadı: ${billingResult.debugMessage}")
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList.first()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    override fun onPurchasesUpdated(
        billingResult: com.android.billingclient.api.BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode != BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            return
        }

        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                onPurchaseAcknowledged?.invoke()
            }
        }
    }
}
