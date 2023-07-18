package com.grace.dayas.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.googleplaysubscription.Print
import java.io.IOException


fun BillingClient.establishConnection(
    requestHandle: (() -> Unit)? = null,
    handleRetry: (() -> Unit)? = null
) {
    startConnection(object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() {
            handleRetry?.invoke()
        }

        override fun onBillingSetupFinished(p0: BillingResult) {
            requestHandle?.invoke()
        }

    })
}

fun productDetailParam(productId: String): QueryProductDetailsParams.Builder {
    val productList = listOf(
        QueryProductDetailsParams.Product.newBuilder().setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
    )
    return QueryProductDetailsParams.newBuilder()
        .setProductList(productList)
}

fun verifyValidSignature(signData: String, signature: String): Boolean {
    return try {
        val security = BillingSecurity()
        val base64Key =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApvejh+CtAaaQkoA2PGVMyB1fLX3lJzgJz/oe1Ij1ewBEW8+lzvbzAOW2msFRw/tsH6jyoYxxABR9yXqcsw+DfYAUwXUnBY/4AmgdQA8z6cXcIWWnsmMTvbWRfmi9KJ0ctd3gc9ALvyKb4nxiZiB0fhgEZp7ezC80kBmfvth45UI+KLIxfSDIazdNnIEFEfhT+eOCU87z/ZjSoKcuzHE1DB06gq6JVP09BQtUK6OWm2nAVPfRDXNFCqmvLdInept6EGo/K/6L0Vl2xO4HKBUqGkyFSbNHSXr2ZPQlLKJGCgoZNkawxQIdI9PN6QZsFK5FtPJc4RG14i5qlpU+VlweSQIDAQAB"
        security.verifyPurchase(base64Key, signData, signature)
    } catch (e: IOException) {
        false
    }
}

fun getSubPriceValidity(billingPeriod: String): Pair<String, String> {

    return when (billingPeriod) {
        "P1W" -> Pair("1", "week")
        "P3W" -> Pair("3", "week")
        "P1M" -> Pair("1", "month")
        "P3M" -> Pair("3", "month")
        "P6M" -> Pair("6", "month")
        "P1Y" -> Pair("12", "month")
        else -> Pair("0", "month")
    }
}

fun Activity.removeSubscription(productId: String) {
    try {
        val subscriptionUrl =
            "http://play.google.com/store/account/subscriptions?package=$packageName&sku=$productId"
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(subscriptionUrl)
        startActivity(intent)
        finish()
    } catch (e: Exception) {
        Print.log("Handling subscription cancellation: error while trying to unsubscribe")
        e.printStackTrace()
    }
}