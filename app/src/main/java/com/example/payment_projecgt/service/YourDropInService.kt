package com.example.payment_projecgt.service

import android.util.Log
import com.adyen.Client
import com.adyen.checkout.components.core.ActionComponentData
import com.adyen.checkout.components.core.PaymentComponentState
import com.adyen.checkout.components.core.action.Action
import com.adyen.checkout.components.core.paymentmethod.CardPaymentMethod
import com.adyen.checkout.dropin.DropInService
import com.adyen.checkout.dropin.DropInServiceResult
import com.adyen.checkout.redirect.RedirectComponent
import com.adyen.enums.Environment
import com.adyen.model.checkout.Amount
import com.adyen.model.checkout.AuthenticationData
import com.adyen.model.checkout.BillingAddress
import com.adyen.model.checkout.BrowserInfo
import com.adyen.model.checkout.CardDetails
import com.adyen.model.checkout.CheckoutPaymentMethod
import com.adyen.model.checkout.PaymentCompletionDetails
import com.adyen.model.checkout.PaymentDetailsRequest
import com.adyen.model.checkout.PaymentDetailsResponse
import com.adyen.model.checkout.PaymentRequest
import com.adyen.model.checkout.PaymentResponse
import com.adyen.model.checkout.ThreeDSRequestData
import com.adyen.service.checkout.PaymentsApi
import com.example.payment_projecgt.constants.ApplicationConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject


class YourDropInService:DropInService() {
    lateinit var response: PaymentResponse
    lateinit var responseDetails: PaymentDetailsResponse
    override fun onSubmit(state: PaymentComponentState<*>) {
        Log.d("check_state","$state")
        makePayment(state)

    }

    override fun onAdditionalDetails(actionComponentData: ActionComponentData) {
        Log.d("check_actdata1","onAdditionalDetails Clicked")
        Log.d("check_actdata2","${actionComponentData}")

        val actionComponentJson = ActionComponentData.SERIALIZER.serialize(actionComponentData)
        Log.d("check_actdata_json","${actionComponentJson}")

        callPaymentDetails(actionComponentJson)

    }

    private fun callPaymentDetails(actionComponentJson: JSONObject) {
        val client = Client(ApplicationConstant.API_KEY, Environment.TEST)
        Log.d("check_details","${actionComponentJson.get("details")}")
        val j = actionComponentJson.get("details") as JSONObject
        Log.d("check_threeDS2Result","${j.get("threeDSResult")}")

       val paymentCompletionDetails =  PaymentCompletionDetails()
            .threeDSResult(actionComponentJson.get("details").toString())
        val paymentCompletionDetails2 =  PaymentCompletionDetails()
            .threeDSResult(j.get("threeDSResult").toString())

        val paymentDetailsRequest:PaymentDetailsRequest = PaymentDetailsRequest()
            .details(paymentCompletionDetails2)


        GlobalScope.launch {
            val service = PaymentsApi(client,ApplicationConstant.BASE_URL)
            callPaymentDetailsApi(service,paymentDetailsRequest)
        }

    }

    private suspend fun callPaymentDetailsApi(
        service: PaymentsApi,
        paymentDetailsRequest: PaymentDetailsRequest
    ) {
        try {
            val job = GlobalScope.launch(Dispatchers.IO){
                responseDetails = service.paymentsDetails(paymentDetailsRequest)
            }
            job.join()
            Log.d("check_res_details","${responseDetails}")
        } catch (e:Exception){
            Log.d("check_res_details_err"," Error = ${e}")
        }
    }

    private fun makePayment(state: PaymentComponentState<*>) {
        val data = state.data
        val paymentData = data.paymentMethod
        val client = Client(ApplicationConstant.API_KEY, Environment.TEST)

        // Example: Assuming you have an instance of CardPaymentMethod
        val cardPaymentMethod = paymentData as CardPaymentMethod
        val serializeJson = CardPaymentMethod.SERIALIZER.serialize(cardPaymentMethod)

        val cardPaymentMethodData = CardPaymentMethod.SERIALIZER.deserialize(jsonObject = serializeJson )

        // Create the request object(s)
        val amount = Amount()
            .currency(data.amount?.currency)
            .value(data.amount?.value)

        val cardDetails = CardDetails()
            .threeDS2SdkVersion(cardPaymentMethodData.threeDS2SdkVersion)
            .encryptedCardNumber(cardPaymentMethodData.encryptedCardNumber)
            .holderName(cardPaymentMethodData.holderName)
            .encryptedSecurityCode(cardPaymentMethodData.encryptedSecurityCode)
            .encryptedExpiryYear(cardPaymentMethodData.encryptedExpiryYear)
            .encryptedExpiryMonth(cardPaymentMethodData.encryptedExpiryMonth)
            .type(CardDetails.TypeEnum.SCHEME)

        val billingAddress: BillingAddress = BillingAddress()
            .country("NL")
            .city("Amsterdam")
            .street("Infinite Loop")
            .houseNumberOrName("1")
            .postalCode("1011DJ")

        val browserInfo: BrowserInfo = BrowserInfo()
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .userAgent("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008052912 Firefox/3.0")
            .javaEnabled(true)
            .colorDepth(10)
            .screenWidth(3000)
            .screenHeight(2000)
            .timeZoneOffset(5)
            .language("en")
        var referenceId = "orderId_1106_63726364"

        val additionalData = mapOf(
            "allow3DS2" to "true",
            "challengeWindowSize" to "02"
        )

        val authenticationData = AuthenticationData()
        val threeDSRequestData = ThreeDSRequestData()
        threeDSRequestData.setNativeThreeDS(ThreeDSRequestData.NativeThreeDSEnum.PREFERRED)
        authenticationData.threeDSRequestData = threeDSRequestData

        var paymentRequest = PaymentRequest()
            .reference(referenceId)
            .amount(amount)
            .merchantAccount(ApplicationConstant.MERCHANT_ACCOUNT)
            .paymentMethod(CheckoutPaymentMethod(cardDetails))
            .returnUrl(RedirectComponent.getReturnUrl(applicationContext))
            .browserInfo(browserInfo)
            .channel(PaymentRequest.ChannelEnum.ANDROID)
            .shopperEmail("rahulsainigoky@gmail.com")
            .billingAddress(billingAddress)
            .shopperReference(data.shopperReference)
            .additionalData(additionalData)
            .authenticationData(authenticationData)




//         Send the request
        GlobalScope.launch {
            val service = PaymentsApi(client,ApplicationConstant.BASE_URL)
            callApi(service,paymentRequest)
        }
    }

    private suspend fun callApi(service: PaymentsApi, paymentRequest: PaymentRequest?) {
        try {
            val job = GlobalScope.launch(Dispatchers.IO){
                response = service.payments(paymentRequest)
            }
            job.join()
            Log.d("check_payapi_res","${response}")
        } catch (e:Exception){
            Log.d("check_payapi_res_err"," Error = ${e}")
        }

        if (response.action.checkoutThreeDS2Action.type.value.equals("threeDS2")){
            var JSON= JSONObject()
            JSON.put("authorisationToken",response.action.checkoutThreeDS2Action.authorisationToken)
            JSON.put("paymentData",response.action.checkoutThreeDS2Action.paymentData)
            JSON.put("paymentMethodType",response.action.checkoutThreeDS2Action.paymentMethodType)
            JSON.put("subtype",response.action.checkoutThreeDS2Action.subtype)
            JSON.put("token",response.action.checkoutThreeDS2Action.token)
            JSON.put("type",response.action.checkoutThreeDS2Action.type)
            JSON.put("url",response.action.checkoutThreeDS2Action.url)
            var actionObject = Action.SERIALIZER.deserialize(JSON)
            Log.d("check_submit_actionjson1","${actionObject.type}")
            Log.d("check_submit_actionjson2","${actionObject.paymentData}")
            Log.d("check_submit_actionjson3","${actionObject.paymentMethodType}")
            //sendResult(DropInServiceResult.Finished("YOUR_RESULT"))
            sendResult(DropInServiceResult.Action(actionObject))
        }
    }

}