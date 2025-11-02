package com.example.payment_projecgt.service

import android.app.Dialog
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.adyen.Client
import com.adyen.checkout.components.core.ActionComponentData
import com.adyen.checkout.components.core.PaymentComponentState
import com.adyen.checkout.components.core.action.Action
import com.adyen.checkout.components.core.paymentmethod.CardPaymentMethod
import com.adyen.checkout.dropin.DropInService
import com.adyen.checkout.dropin.DropInServiceResult
import com.adyen.checkout.dropin.ErrorDialog
import com.adyen.enums.Environment
import com.adyen.model.checkout.Amount
import com.adyen.model.checkout.AuthenticationData
import com.adyen.model.checkout.BillingAddress
import com.adyen.model.checkout.BrowserInfo
import com.adyen.model.checkout.CardDetails
import com.adyen.model.checkout.CheckoutPaymentMethod
import com.adyen.model.checkout.DetailsRequestAuthenticationData
import com.adyen.model.checkout.PaymentCompletionDetails
import com.adyen.model.checkout.PaymentDetailsRequest
import com.adyen.model.checkout.PaymentDetailsResponse
import com.adyen.model.checkout.PaymentRequest
import com.adyen.model.checkout.PaymentResponse
import com.adyen.model.checkout.ThreeDSRequestData
import com.adyen.service.checkout.PaymentsApi
import com.example.payment_projecgt.constants.ApplicationConstant
import com.example.payment_projecgt.data.AppSharedPref
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class YourDropInService:DropInService() {
    lateinit var response: PaymentResponse
    lateinit var paymentDetails: PaymentDetailsResponse
    override fun onSubmit(state: PaymentComponentState<*>) {
        Log.d("check_state","$state")
        makePayment(state)
    }

    private fun makePayment(state: PaymentComponentState<*>) {
        val data = state.data
        Log.d("check_reference","PaymentComponentState data -> ${state.data.shopperReference}")
        val paymentData = data.paymentMethod
        val environment = if (AppSharedPref.getAdyenTestMode(applicationContext) == "1") {
            Environment.TEST
        } else {
            Environment.LIVE
        }

        val client = Client(AppSharedPref.getAdyenApiKey(this), environment)
//        val client = Client(AppSharedPref.getAdyenApiKey(this), Environment.TEST)

        // Example: Assuming you have an instance of CardPaymentMethod
        val cardPaymentMethod = paymentData as CardPaymentMethod
        val serializeJson = CardPaymentMethod.SERIALIZER.serialize(cardPaymentMethod)

        val cardPaymentMethodData = CardPaymentMethod.SERIALIZER.deserialize(jsonObject = serializeJson )
        Log.d("check_card_data","card payment data 1 ${cardPaymentMethodData}")
        Log.d("check_card_data","card payment data 2 ${Gson().toJson(cardPaymentMethodData)}")
        Log.d("check_card_data","card payment data 2 ${cardPaymentMethodData.threeDS2SdkVersion}")

        // Create the request object(s)
        Log.d("check_currency","${data.amount?.currency}")
        Log.d("check_amount","${data.amount?.value}")
        val amount = Amount()
            .currency(data.amount?.currency)
            .value(data.amount?.value!!)
//            .value(data.amount?.value?.toDouble()?.div(100)?.toLong())
        Log.d("check_amount_value","amount in service -> ${amount}")
        val cardDetails = CardDetails()
            .threeDS2SdkVersion(cardPaymentMethodData.threeDS2SdkVersion)
            .encryptedCardNumber(cardPaymentMethodData.encryptedCardNumber)
            .holderName(cardPaymentMethodData.holderName)
            .encryptedSecurityCode(cardPaymentMethodData.encryptedSecurityCode)
            .encryptedExpiryYear(cardPaymentMethodData.encryptedExpiryYear)
            .encryptedExpiryMonth(cardPaymentMethodData.encryptedExpiryMonth)
            .type(CardDetails.TypeEnum.SCHEME)
            .storedPaymentMethodId(cardPaymentMethodData.storedPaymentMethodId)


//        val billingAddress: BillingAddress = BillingAddress()
//            .country("TO")
//            .city("Nuku'alofa")
//            .street("Infinite Loop")
//            .houseNumberOrName("1")
//            .postalCode("Tonga")
//
//        val browserInfo: BrowserInfo = BrowserInfo()
//            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
//            .userAgent("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008052912 Firefox/3.0")
//            .javaEnabled(true)
//            .colorDepth(10)
//            .screenWidth(3000)
//            .screenHeight(2000)
//            .timeZoneOffset(5)
//            .language("en")

        val additionalData = mapOf(
            "allow3DS2" to "true",
            "challengeWindowSize" to "02"
        )

        val authenticationData = AuthenticationData()
        val threeDSRequestData = ThreeDSRequestData()

        threeDSRequestData.setNativeThreeDS(ThreeDSRequestData.NativeThreeDSEnum.PREFERRED)
//        threeDSRequestData.threeDSVersion = ThreeDSRequestData.ThreeDSVersionEnum._2_0
        authenticationData.threeDSRequestData = threeDSRequestData
        authenticationData.attemptAuthentication = AuthenticationData.AttemptAuthenticationEnum.ALWAYS
        Log.d("check_amount","amount -> ${Gson().toJson(amount)}")
        var paymentRequest = PaymentRequest()
            .reference("345654334")    // provide unique during each payment
            .amount(amount)
            .merchantAccount(AppSharedPref.getAdyenMerchantId(this))
            .paymentMethod(CheckoutPaymentMethod(cardDetails))
            //.returnUrl(RedirectComponent.getReturnUrl(applicationContext))
            .returnUrl("adyencheckout://com.example.payment_projecgt")
            //.browserInfo(browserInfo)
            .channel(PaymentRequest.ChannelEnum.ANDROID)
            //.shopperEmail("rahulsainigoku@gmail.com")
            //.billingAddress(billingAddress)
            .shopperReference(data.shopperReference)
            .additionalData(additionalData)
            .authenticationData(authenticationData)
//            .enablePayOut(true)
//            .enableRecurring(true)
            .storePaymentMethod(true)
            .recurringProcessingModel(PaymentRequest.RecurringProcessingModelEnum.CARDONFILE)

        Log.d("check_payment_req","${paymentRequest}")



        //         Send the request
        GlobalScope.launch {
            try {
                val baseUrl = if (AppSharedPref.getAdyenTestMode(applicationContext) == "1") {
                    ApplicationConstant.PAYMENT_API_BASE_URL_TEST
                } else {
                    ApplicationConstant.PAYMENT_API_BASE_URL_LIVE
                }

                val service = PaymentsApi(client, baseUrl)
//                val service = PaymentsApi(client, ApplicationConstants.PAYMENT_API_BASE_URL)
                callApi(service,paymentRequest)
            } catch (e:Exception){
                Log.d("check_error","${e.message}")
            }
        }
    }

    private suspend fun callApi(service: PaymentsApi, paymentRequest: PaymentRequest?) {
        try {
            val job = GlobalScope.launch(Dispatchers.IO){
                response = service.payments(paymentRequest)
            }
            job.join()
            Log.d("check_payapi_res"," adyen payment api response -> ${response}")
            if (response.resultCode == PaymentResponse.ResultCodeEnum.IDENTIFYSHOPPER){
                if (response?.action?.checkoutThreeDS2Action?.type?.value.equals("threeDS2")){
                    Log.d("check_action_only","${response.action}")
                    var JSON= JSONObject()
                    JSON.put("authorisationToken",response.action.checkoutThreeDS2Action.authorisationToken)
                    JSON.put("paymentData",response.action.checkoutThreeDS2Action.paymentData)
                    JSON.put("paymentMethodType",response.action.checkoutThreeDS2Action.paymentMethodType)
                    JSON.put("subtype",response.action.checkoutThreeDS2Action.subtype)
                    JSON.put("token",response.action.checkoutThreeDS2Action.token)
                    JSON.put("type",response.action.checkoutThreeDS2Action.type)
                    JSON.put("url",response.action.checkoutThreeDS2Action.url)
                    var actionObject = Action.SERIALIZER.deserialize(JSON)
                    Log.d("check_action_type","payment type -> ${actionObject.type}")
                    Log.d("check_action_paymentData","payment data -> ${actionObject.paymentData}")
                    Log.d("check_action_pay_meth_type","action payment method type -> ${actionObject.paymentMethodType}")
                    //sendResult(DropInServiceResult.Finished("YOUR_RESULT"))
                    sendResult(DropInServiceResult.Action(actionObject))
                }
            } else{
                val gson = Gson().toJson(response)
                Log.d("check_payapi_res_auth","payment authorised response -> ${gson}")
                sendResult(DropInServiceResult.Finished(gson))
            }
        } catch (e:Exception){
            Log.d("check_payapi_res_err"," Error = ${e}")
        }
    }

    override fun onAdditionalDetails(actionComponentData: ActionComponentData) {
        Log.d("check_act_comp_data","actionComponentData -> ${actionComponentData}")
        val actionComponentJson = ActionComponentData.SERIALIZER.serialize(actionComponentData)
        val environment = if (AppSharedPref.getAdyenTestMode(this) == "1") {
            Environment.TEST
        } else {
            Environment.LIVE
        }

        val client = Client(AppSharedPref.getAdyenApiKey(this), environment)
//        val client = Client(AppSharedPref.getAdyenApiKey(this), Environment.TEST)
        val detailsJson = actionComponentJson.get("details") as JSONObject
        Log.d("check_details_json","details json -> ${detailsJson}")
        val details = PaymentCompletionDetails()
            .threeDSResult(detailsJson.getString("threeDSResult"))


        val paymentDetailsRequest = PaymentDetailsRequest()
            .details(details)
            .authenticationData(DetailsRequestAuthenticationData().authenticationOnly(true))


        //         Send the request
        GlobalScope.launch {
            val baseUrl = if (AppSharedPref.getAdyenTestMode(applicationContext) == "1") {
                ApplicationConstant.PAYMENT_API_BASE_URL_TEST
            } else {
                ApplicationConstant.PAYMENT_API_BASE_URL_LIVE
            }

            val service = PaymentsApi(client, baseUrl)

//            val service = PaymentsApi(client, ApplicationConstants.PAYMENT_API_BASE_URL)
            callPaymentDetailsApi(service,paymentDetailsRequest)
        }
    }

    private suspend fun callPaymentDetailsApi(service: PaymentsApi, paymentDetailsRequest:PaymentDetailsRequest) {

        try {
            val job = GlobalScope.launch(Dispatchers.IO){
                paymentDetails = service.paymentsDetails(paymentDetailsRequest)
            }
            job.join()
            val gson = Gson().toJson(paymentDetails)
            Log.d("check_pay_details_res","payment details api response -> ${gson}")
//            saveToSharedPreference(paymentDetails)
            when(paymentDetails.resultCode.value){
                "Authorised" -> {
                    sendResult(DropInServiceResult.Finished(gson))
                }
                "Error" -> {
                    sendResult(DropInServiceResult.Error(ErrorDialog("Error","Something went wrong")))
                }
                else -> {
                    sendResult(DropInServiceResult.Error(ErrorDialog("Error","Something went wrong")))
                    Log.d("check_other_reason","${paymentDetails.resultCode.value}")
                }
            }
        } catch (e:Exception){
            Log.d("check_payapi_res_err"," Error = ${e}")
        }
    }
}