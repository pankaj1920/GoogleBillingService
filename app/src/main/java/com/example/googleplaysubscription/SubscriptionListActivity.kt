package com.example.googleplaysubscription

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.googleplaysubscription.databinding.ActivitySubscriptionListBinding
import java.io.IOException
import java.util.concurrent.Executors

class SubscriptionListActivity : AppCompatActivity() {
    private lateinit var itemArrayList: ArrayList<SubItemModel>
    var isSuccess = false
    var productId = 0
    private var billingClient: BillingClient? = null

    lateinit var binding: ActivitySubscriptionListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemArrayList = arrayListOf()

        billingClient = BillingClient.newBuilder(this)
            .setListener(purcahseUpdatedListner)
            .enablePendingPurchases()
            .build()

        showSubscriptionList()
    }

    private fun showSubscriptionList() {
        billingClient!!.startConnection(object : BillingClientStateListener,
            SubscriptionListAdapter.OnItemClickListener {
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute {
                    val productList = listOf(
                        QueryProductDetailsParams.Product.newBuilder().setProductId("")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)

                    billingClient!!.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->

                        for (productDetail in productDetailsList) {
                            if (productDetail.subscriptionOfferDetails != null) {
                                for (i in 0 until productDetail.subscriptionOfferDetails!!.size) {
                                    var subName = productDetail.name
                                    var index = i
                                    var phases = ""
                                    var formattedPrice =
                                        productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[0].formattedPrice.toString()
                                    var billingPeriod =
                                        productDetail.subscriptionOfferDetails!![i].pricingPhases.pricingPhaseList[0].billingPeriod.toString()

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

                                            subName += "\n" + productDetail.subscriptionOfferDetails!!.get(
                                                i
                                            ).offerId.toString()
                                            phases += "\n$offerPrice $offerPeriod"
                                        }
                                    }

                                    itemArrayList.add(SubItemModel(subName, phases, index))
                                }
                            }
                        }

                    }
                }


                runOnUiThread {
                    Thread.sleep(1000)

                    var adapter = SubscriptionListAdapter(itemArrayList)
                    binding.rvSubscription.adapter = adapter
                    adapter.setOnItemClickListener(this)
                }
            }

            override fun onItemClick(position: Int) {
                val item = itemArrayList[position]
                productId = item.planIndex

                subscribeProduct()
            }

        })
    }

    fun subscribeProduct() {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productList = listOf(
                        QueryProductDetailsParams.Product.newBuilder().setProductId("")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)

                    billingClient!!.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->

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

                            val billingResult = billingClient!!.launchBillingFlow(
                                this@SubscriptionListActivity,
                                billingFlowParams
                            )

                        }
                    }
                }
            }

        })
    }

    private val purcahseUpdatedListner = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(this, "Already Subscribed", Toast.LENGTH_SHORT).show()
            isSuccess = true
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            Toast.makeText(this, "Feature Not Supported", Toast.LENGTH_SHORT).show()

        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "User Cancelled", Toast.LENGTH_SHORT).show()

        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
            Toast.makeText(this, "Billing Unavailable", Toast.LENGTH_SHORT).show()

        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR) {
            Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(this, " Error ${billingResult.debugMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun handlePurchase(purchase: Purchase) {
        val consumeParams =
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()

        val listener = ConsumeResponseListener { billingResult, s ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

            }
        }

        billingClient!!.consumeAsync(consumeParams, listener)
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                Toast.makeText(this, "Invalid Purchase", Toast.LENGTH_SHORT).show()
                return
            }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()

                billingClient!!.acknowledgePurchase(
                    acknowledgePurchaseParams,
                    acknowledgePurchaseResponseListner
                )
                isSuccess = true
            } else {
                Toast.makeText(this, "Already Subscribed", Toast.LENGTH_SHORT).show()
                return
            }
        }else if (purchase.purchaseState == Purchase.PurchaseState.PENDING){
            Toast.makeText(this, "Purchase Pending", Toast.LENGTH_SHORT).show()
        }else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE){
            Toast.makeText(this, "UnSpecified State", Toast.LENGTH_SHORT).show()
        }
    }

    var acknowledgePurchaseResponseListner = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isSuccess = true
        }
    }

    private fun verifyValidSignature(signData: String, signature: String): Boolean {
        return try {
            val security = Security()
            val base64Key = ""
            security.verifyPurchase(base64Key, signData, signature)
        } catch (e: IOException) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (binding!= null){
            billingClient?.endConnection()
        }
    }
}