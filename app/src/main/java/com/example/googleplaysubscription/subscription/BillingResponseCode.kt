package com.grace.dayas.subscription

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.grace.dayas.utils.Print

fun handleBillingResultError(billingResult: BillingResult, msg: String? = null): String {
    msg.let { Print.log("handleBillingResultError ==> $msg") }
    var errorMessage = ""
    when (billingResult.responseCode) {
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
            errorMessage = "onPurchasesUpdated: Developer error means that Google Play does " +
                    "not recognize the configuration. If you are just getting started, " +
                    "make sure you have configured the application correctly in the " +
                    "Google Play Console. The product ID must match and the APK you " +
                    "are using must be signed with release keys."
            Print.log(errorMessage)
        }

        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
            errorMessage = "Already Subscribed"
            Print.log("Already Subscribed")
        }

        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
            errorMessage = "Feature Not Supported"
            Print.log("Feature Not Supported")
        }

        BillingClient.BillingResponseCode.USER_CANCELED -> {
            errorMessage = "User Cancelled"
            Print.log("User Cancelled")
        }

        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
            errorMessage = "Billing Unavailable"
            Print.log("Billing Unavailable")
        }

        BillingClient.BillingResponseCode.NETWORK_ERROR -> {
            errorMessage = "Network Error"
            Print.log("Network Error")
        }

        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
            errorMessage = "Service Unavailable"
            Print.log("Service Unavailable")
        }

        else -> {
            errorMessage = "Unexpected Error ${billingResult.debugMessage}"
            Print.log("Unexpected Error ${billingResult.debugMessage}")
        }
    }

    return errorMessage
}