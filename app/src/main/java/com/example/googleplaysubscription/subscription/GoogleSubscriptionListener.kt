package com.grace.dayas.subscription

import com.android.billingclient.api.BillingResult

interface GetSubscriptionListListener {
    fun getSubscriptionList(subscriptionList: ArrayList<BillingProductItemModel>)
    fun onSubscriptionListError(billingResult: BillingResult, errorCode: Int, errorMessage: String)
    fun onSubscriptionPurchaseSuccess(purchaseInfo:String)
    fun onSubscriptionPurchaseError(errorCode: Int,errorMessage: String,errorType:String?)
}
