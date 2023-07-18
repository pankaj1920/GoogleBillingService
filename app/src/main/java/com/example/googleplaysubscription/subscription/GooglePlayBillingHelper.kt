package com.grace.dayas.subscription

import android.app.Activity
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.grace.dayas.utils.Print
import java.util.concurrent.Executors

class GooglePlayBillingHelper(private val context: Activity,listener: GetSubscriptionListListener) : PurchasesUpdatedListener,
    AcknowledgePurchaseResponseListener {
    private var billingClient: BillingClient
    private lateinit var subscriptionItemList: ArrayList<BillingProductItemModel>
    private var productId = 0
    lateinit var subscriptionListener: GetSubscriptionListListener

    init {
        subscriptionListener = listener
        Print.log("Initialize a BillingClient")
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()


    }


    fun showSubscriptionList() {

        Print.log("showSubscriptionList")
        billingClient.establishConnection(requestHandle = ::getSubscriptionList)
    }

    private fun getSubscriptionList() {
        subscriptionItemList = ArrayList()
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val params = productDetailParam(productId = "basic")

            billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->

                if (billingResult.responseCode == OK) {
                    for (productDetail in productDetailsList) {
                        Print.log("Product Detail $productDetail")
                        if (productDetail.subscriptionOfferDetails != null) {
                            for (productItem in 0 until productDetail.subscriptionOfferDetails!!.size) {

                                val subscriptionOfferDetail =
                                    productDetail.subscriptionOfferDetails!![productItem]

                                for (itemPrice in 0 until (subscriptionOfferDetail.pricingPhases.pricingPhaseList.size)) {
                                    val subscriptionItem =
                                        productDetail.subscriptionOfferDetails!![productItem].pricingPhases.pricingPhaseList[itemPrice]
                                    Print.log("Product Items ==> subscriptionName ${subscriptionOfferDetail.basePlanId} Price:${subscriptionItem.formattedPrice}")
                                    subscriptionItemList.add(
                                        BillingProductItemModel(
                                            subscriptionName = subscriptionOfferDetail.basePlanId,
                                            formattedPrice = subscriptionItem.formattedPrice,
                                            priceAmountMicros = subscriptionItem.priceAmountMicros,
                                            priceCurrencyCode = subscriptionItem.priceCurrencyCode,
                                            billingPeriod = subscriptionItem.billingPeriod,
                                            recurrenceMode = subscriptionItem.recurrenceMode,
                                            productItem
                                        )
                                    )
                                }


                            }

                            subscriptionListener.getSubscriptionList(subscriptionItemList)
                        }
                    }
                } else {
                    val errorMessage =
                        handleBillingResultError(billingResult, "getSubscriptionList")
                    subscriptionListener.onSubscriptionListError(
                        billingResult,
                        billingResult.responseCode,
                        errorMessage
                    )
                }


            }
        }

    }

    fun buyProduct(id: Int) {
        Print.log("buyProduct ProductId $id")
        productId = id
        billingClient.establishConnection(::subscribeProduct)
    }

    private fun subscribeProduct() {
        Print.log("subscribeProduct")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId("basic")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)

        billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
            for (productDetail in productDetailsList) {
                val offerToken =
                    productDetail.subscriptionOfferDetails?.get(productId)?.offerToken

                val productDetailParamList = listOf(
                    offerToken?.let {
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetail)
                            .setOfferToken(it)
                            .build()
                    }
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailParamList)
                    .build()

                val billingResult = billingClient.launchBillingFlow(
                    context,
                    billingFlowParams
                )

                Print.log("subscribeProduct billingResult Code ${billingResult.responseCode} and Message ${billingResult.debugMessage}")
            }
        }
    }


    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == OK && purchases != null) {
            for (purchase in purchases) {
                Print.log("Got a purchase: $purchase; but signature is bad. Skipping...")
                // When every a new purchase is made
                // Here we verify our purchase
                handlePurchase(purchase)
            }
        } else {
            val errorMessage = handleBillingResultError(billingResult, "onPurchasesUpdated")
            subscriptionListener.onSubscriptionPurchaseError(
                101,
                errorMessage,
                "onPurchasesUpdatedError"
            )

        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                    Print.log("Invalid Purchase")
                    Toast.makeText(context, "Invalid Purchase", Toast.LENGTH_SHORT).show()
                    return
                }

                if (!purchase.isAcknowledged) {

                    subscriptionListener.onSubscriptionPurchaseSuccess(purchase.originalJson)

                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)



                } else {
                    subscriptionListener.onSubscriptionPurchaseError(
                        101,
                        "Already Subscribed",
                        "handlePurchaseError"
                    )
                    Print.log("Already Subscribed")
                    return
                }

            }
            Purchase.PurchaseState.PENDING -> {
                subscriptionListener.onSubscriptionPurchaseError(
                    101,
                    "Purchase Pending",
                    "handlePurchaseError"
                )
                Print.log("Purchase Pending")
            }
            Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                subscriptionListener.onSubscriptionPurchaseError(
                    101,
                    "UnSpecified State",
                    "handlePurchaseError"
                )
                Print.log("UnSpecified State")
            }
        }
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        Print.log("onAcknowledgePurchaseResponse BillingResult ${billingResult}")

        when (billingResult.responseCode) {
            OK -> {
                Print.log("Acknowledge success")
            }

            else -> {
                val errorMessage =
                    handleBillingResultError(billingResult, "onAcknowledgePurchaseResponse")
                subscriptionListener.onSubscriptionPurchaseError(
                    101,
                    errorMessage,
                    "onAcknowledgePurchaseError"
                )
            }


        }
    }

    fun queryPurchase() {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute {
                    billingClient!!.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS).build()
                    ){billingResult,purchaseList->
                        for (purchase in purchaseList){
                            if (purchase!=null && purchase.isAcknowledged){
                             Print.log("Purchased Item $purchase")

                                for (i in 0 until purchase.products.size){
                                   val proName =purchase.products[i].toString()
                                    Print.log("Purchased Product Bane $proName")
                                }
                            }
                        }
                    }
                }

            }

        })
    }



}
