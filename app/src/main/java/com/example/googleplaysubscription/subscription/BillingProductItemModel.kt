package com.grace.dayas.subscription

data class BillingProductItemModel(
    val subscriptionName: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriod: String,
    /*
    * recurrenceMode = 1 (INFINITE_RECURRING)  => The billing plan payment recurs for infinite billing periods unless cancelled.
    * recurrenceMode = 2 (FINITE_RECURRING) => The billing plan payment recurs for a fixed number of billing period set in billingCycleCount.
    * recurrenceMode = 3 (NON_RECURRING) => The billing plan payment is a one time charge that does not repeat.
    * */
    val recurrenceMode: Int,
    val planIndex: Int
)