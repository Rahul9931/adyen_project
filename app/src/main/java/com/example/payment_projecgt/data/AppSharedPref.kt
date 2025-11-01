package com.example.payment_projecgt.data

import android.content.Context
import android.content.SharedPreferences

object AppSharedPref {
    private const val APP_PREF = "app_preferences"
    private const val KEY_ADYEN_TEST_MODE = "key_adyen_test_mode"
    private const val KEY_ADYEN_API_KEY = "key_adyen_api_key"
    private const val KEY_ADYEN_CLIENT_KEY = "key_adyen_client_key"
    private const val KEY_ADYEN_MERCHANT_ID = "key_adyen_merchant_id"

    fun getPref(context: Context): SharedPreferences{
        return context.getSharedPreferences(APP_PREF, Context.MODE_PRIVATE)
    }

    fun setAdyenTestMode(context: Context, isTestMode: String){
        getPref(context).edit().apply {
            putString(KEY_ADYEN_TEST_MODE,isTestMode)
            apply()
        }
    }

    fun getAdyenTestMode(context: Context): String? {
        return getPref(context).getString(KEY_ADYEN_TEST_MODE,"0")
    }

    fun setAdyenApiKey(context: Context, apikey: String){
        getPref(context).edit().apply{
            putString(KEY_ADYEN_API_KEY,apikey)
            apply()
        }
    }

    fun getAdyenApiKey(context: Context): String? {
        return getPref(context).getString(KEY_ADYEN_API_KEY,"")
    }

    fun setAdyenClientKey(context: Context, clientKey: String){
        getPref(context).edit().apply{
            putString(KEY_ADYEN_CLIENT_KEY,clientKey)
            apply()
        }
    }

    fun getAdyenClientKey(context: Context): String? {
        return getPref(context).getString(KEY_ADYEN_CLIENT_KEY,"")
    }

    fun setAdyenMerchantId(context: Context, merchantId: String){
        getPref(context).edit().apply{
            putString(KEY_ADYEN_MERCHANT_ID,merchantId)
            apply()
        }
    }

    fun getAdyenMerchantId(context: Context): String? {
        return getPref(context).getString(KEY_ADYEN_MERCHANT_ID,"")
    }

}