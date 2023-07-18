package com.example.googleplaysubscription

import android.util.Log
import com.android.billingclient.BuildConfig


object Print {
    fun log(message: String?) {

            println("##################################")
            println("Print : $message")
            Log.e("Print", "Print : $message")


    }
}