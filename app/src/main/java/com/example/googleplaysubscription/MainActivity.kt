package com.example.googleplaysubscription

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.example.googleplaysubscription.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var billingClient: BillingClient? = null
    var isRemoveAds = false
    var isSuccess = false
    var proName  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        billingClient = BillingClient.newBuilder(this).setListener(purcahseUpdatedListner)
            .enablePendingPurchases().build()

        queryPurchase()

        binding.btnSub.setOnClickListener {
            startActivity(Intent(this, SubscriptionListActivity::class.java))
        }
    }


    private val purcahseUpdatedListner = PurchasesUpdatedListener { billingResult, purchases ->

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
                                isSuccess = true
                                isRemoveAds = true

                                for (i in 0 until purchase.products.size){
                                    proName +=purchase.products[i].toString()
                                    val purchaseJson = purchase.originalJson
                                }
                            }
                        }
                    }
                }

                runOnUiThread {
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    binding.tvSubType.text = proName
                }
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()

        if (billingClient!=null){
            billingClient!!.endConnection()
        }
    }

}