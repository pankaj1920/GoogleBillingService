package com.example.googleplaysubscription.google_play_subscription

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.googleplaysubscription.Print
import com.example.googleplaysubscription.SubItemModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GoogleBillingClient(
    private val context: Context,
    private val externalScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : DefaultLifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener,
    ProductDetailsResponseListener {

    private lateinit var billingClient: BillingClient

    override fun onCreate(owner: LifecycleOwner) {
        Print.log("ON_CREATE")

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startBillingConnection()


    }

    fun startBillingConnection() {
        if (!billingClient.isReady) {
            Print.log("BillingClient: Start connection...")
            billingClient.startConnection(this)
        }
    }

    fun showSubscriptionList() {
        startBillingConnection()
    }

    //PurchasesUpdatedListener
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {

    }


    override fun onBillingServiceDisconnected() {

    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Print.log("onBillingSetupFinished: $responseCode $debugMessage")
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The BillingClient is ready. You can query purchases here.
            querySubscriptionProductDetails()
        }


    }

    private fun querySubscriptionProductDetails() {
        Print.log("querySubscriptionProductDetails")
        externalScope.launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId("")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)

            billingClient.queryProductDetailsAsync(params.build(), this@GoogleBillingClient)
        }


    }

    //ProductDetailsResponseListener
    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        productDetailsList: MutableList<ProductDetails>,
    ) {
        val response = BillingResponse(billingResult.responseCode)
        val debugMessage = billingResult.debugMessage

        Print.log("onProductDetailsResponse: $response $debugMessage")
        when {
            response.isOk -> processProductDetails(productDetailsList)

            response.isTerribleFailure -> {
                // These response codes are not expected.
                Print.log("onProductDetailsResponse - Unexpected error: ${response.code} $debugMessage")
            }

            else -> {
                Print.log("onProductDetailsResponse: ${response.code} $debugMessage")
            }

        }


    }

    private fun processProductDetails(productDetailsList: MutableList<ProductDetails>) {
        if (productDetailsList.isEmpty()) {
            Print.log(
                "processProductDetails: Found null ProductDetails. " +
                        "Check to see if the products you requested are correctly published " +
                        "in the Google Play Console."
            )
        } else {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId("")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

            billingClient!!.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->

                for (productDetail in productDetailsList) {
                    if (productDetail.subscriptionOfferDetails != null) {
                        for (i in 0 until productDetail.subscriptionOfferDetails!!.size) {
                            // subscription name
                            var subName = productDetail.name
                            var index = i
                            var phases = ""
                            var formattedPrice =
                                productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[0].formattedPrice
                            var billingPeriod =
                                productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[0].billingPeriod

                            var recurrenceMode: String =
                                productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[0].recurrenceMode.toString()

                            if (recurrenceMode == "2") {
                                when (billingPeriod) {
                                    "P1M" -> billingPeriod = " For 1 Month"
                                    "P6M" -> billingPeriod = " For 6 Month"
                                    "P1Y" -> billingPeriod = " For 1 Year"
                                    "P1W" -> billingPeriod = " For 1 Week"
                                    "P3W" -> billingPeriod = "For 3 Week"
                                }
                            } else {
                                when (billingPeriod) {
                                    "P1M" -> billingPeriod = " /Month"
                                    "P6M" -> billingPeriod = " /Every 6 Month"
                                    "P1Y" -> billingPeriod = " /Year"
                                    "P1W" -> billingPeriod = " /Week"
                                    "P3W" -> billingPeriod = "/Every 3 Week"
                                }
                            }

                            phases = "$formattedPrice/$billingPeriod"

                            for (j in 0 until (productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList.size)) {
                                // here we are putting condition in greater than 0 because in 0 position we will get base offer which we have taken above

                                if (j > 0) {
                                    val offerPeriod =
                                        productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[j].billingPeriod
                                    val offerPrice =
                                        productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[j].formattedPrice

                                    when (offerPeriod) {
                                        "P1M" -> billingPeriod = " /Month"
                                        "P6M" -> billingPeriod = " /Every 6 Month"
                                        "P1Y" -> billingPeriod = " /Year"
                                        "P1W" -> billingPeriod = " /Week"
                                        "P3W" -> billingPeriod = "/Every 3 Week"
                                    }

                                    subName += "\n" + productDetail.subscriptionOfferDetails!![i].offerId.toString()
                                    phases += "\n$offerPrice $offerPeriod"
                                }
                            }

//                            itemArrayList.add(SubItemModel(subName, phases, index))
                        }
                    }else{
                        Print.log("No product found")
                    }
                }

                //show in adapter listner

            }
        }
    }
}