package com.example.payment_projecgt.fragment

// Adyen Java API Library v27.0.0

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.adyen.Client
import com.adyen.checkout.adyen3ds2.adyen3DS2
import com.adyen.checkout.card.card
import com.adyen.checkout.components.core.CheckoutConfiguration
import com.adyen.checkout.components.core.PaymentMethodsApiResponse
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInCallback
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.DropInResult
import com.adyen.checkout.dropin.dropIn
import com.adyen.enums.Environment
import com.adyen.model.checkout.Amount
import com.adyen.model.checkout.PaymentDetailsResponse
import com.adyen.model.checkout.PaymentMethodsRequest
import com.adyen.model.checkout.PaymentMethodsResponse
import com.adyen.service.checkout.PaymentsApi
import com.example.payment_projecgt.R
import com.example.payment_projecgt.constants.ApplicationConstant
import com.example.payment_projecgt.data.AppSharedPref
import com.example.payment_projecgt.databinding.FragmentPaymentBinding
import com.example.payment_projecgt.service.YourDropInService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale


class PaymentFragment : Fragment(), DropInCallback {
    lateinit var binding:FragmentPaymentBinding
    lateinit var paymentResponse:PaymentMethodsResponse
    // Register DropIn Launcher
    private val dropInLauncher = DropIn.registerForDropInResult(this, this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_payment,container,false)

        binding.button.setOnClickListener {
            callPaymentMethod(
                ApplicationConstant.MERCHANT_ACCOUNT,
                ApplicationConstant.API_KEY,
                ApplicationConstant.CLIENT_KEY,
                "1",
                "123456789"
            )
        }

        return binding.root
    }

    private fun callPaymentMethod(
        merchantAccount: String?,
        apiKey: String?,
        clientKey: String?,
        testmode: String?,
        reservedOrderId: String?
    ) {
        AppSharedPref.setAdyenApiKey(requireContext(), ApplicationConstant.API_KEY)
        AppSharedPref.setAdyenClientKey(requireContext(), ApplicationConstant.CLIENT_KEY)
        AppSharedPref.setAdyenMerchantId(requireContext(), ApplicationConstant.MERCHANT_ACCOUNT)
        AppSharedPref.setAdyenTestMode(requireContext(), "1")

        val environment = if (AppSharedPref.getAdyenTestMode(requireContext()) == "1") {
            com.adyen.enums.Environment.TEST
        } else {
            com.adyen.enums.Environment.LIVE
        }

        val client = Client(apiKey, environment)

        // Create the request object(s)

        val amount = Amount()
            .currency("AED")
//            .value(orderTotalValue.toLong())
            .value((123.toDouble() * 100).toLong())
        val paymentMethodsRequest = PaymentMethodsRequest()
            .amount(amount)
            .merchantAccount(merchantAccount)
//            .countryCode(placeOrderData.adyenData?.countryCode)
            .countryCode("AE")
            .channel(PaymentMethodsRequest.ChannelEnum.ANDROID)
//            .shopperLocale(placeOrderData.adyenData?.countryCode)
            .shopperLocale("en_AE")

        // Send the request
//        val service = PaymentsApi(client, ApplicationConstants.PAYMENT_API_BASE_URL)
        val baseUrl = if (AppSharedPref.getAdyenTestMode(requireContext()) == "1") {
            ApplicationConstant.PAYMENT_API_BASE_URL_TEST
        } else {
            ApplicationConstant.PAYMENT_API_BASE_URL_LIVE
        }

        val service = PaymentsApi(client, baseUrl)
        GlobalScope.launch {
            callApi(service,paymentMethodsRequest,reservedOrderId,"AED", 123.00f)
        }
    }

    private suspend fun callApi(
        service: PaymentsApi,
        paymentMethodsRequest: PaymentMethodsRequest?,
        reservedOrderId: String?,
        currencyCode: String,
        orderTotalValue: Float
    ) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            Log.d("check_res"," payment method api req -> ${Gson().toJson(paymentMethodsRequest)}")
            val response = service.paymentMethods(paymentMethodsRequest)
            Log.d("check_res"," payment method api res -> ${response}")
            paymentResponse = response
        }
        job.join()

        val gson = Gson()
        val jsonResponse = JSONObject(gson.toJsonTree(paymentResponse).asJsonObject.toString())

        // Filter out Google Pay before processing
        val filteredJsonResponse = filterOutGooglePay(jsonResponse)

        paymentStart(filteredJsonResponse, reservedOrderId, currencyCode, orderTotalValue)
    }

    private fun paymentStart(
        paymentResponse: JSONObject,
        reservedOrderId: String?,
        currencyCode: String,
        orderTotalValue: Float
    ) {
        val paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(paymentResponse)
        Log.d("check_payapi","PaymentMethodsApiResponse formatted -> ${paymentMethodsApiResponse}")

        // Log the filtered payment methods for verification
        paymentMethodsApiResponse.paymentMethods?.forEach { paymentMethod ->
            Log.d("check_payment", "Final payment method type: ${paymentMethod.type}, name: ${paymentMethod.name}")
        }

        val amount = com.adyen.checkout.components.core.Amount(
            currency = currencyCode,
            value = (orderTotalValue.toDouble() * 100).toLong()
//            value = (orderTotalValue.toDouble() * 100).toLong()
        )
        Log.d("check_payment","Final amount value -> ${Gson().toJson(amount)}")

        // Use dynamic environment based on your settings
        val environment = if (AppSharedPref.getAdyenTestMode(requireContext()) == "1") {
            com.adyen.checkout.core.Environment.TEST
        } else {
            com.adyen.checkout.core.Environment.EUROPE
        }

        val checkoutConfiguration = CheckoutConfiguration(
            environment = environment,
            clientKey = AppSharedPref.getAdyenClientKey(requireContext())!!,
            shopperLocale = Locale("en", "IN"),
            amount = amount
        ) {
            dropIn {
                setEnableRemovingStoredPaymentMethods(true)

            }


            card {
                setHolderNameRequired(true)
                if (reservedOrderId != null) {
                    setShopperReference(reservedOrderId)
                }
            }


            // Important: Do NOT add Google Pay configuration
            // This ensures Google Pay won't be available even if it somehow appears in the list
        }

        DropIn.startPayment(
            requireContext(),
            dropInLauncher,
            paymentMethodsApiResponse,
            checkoutConfiguration,
            YourDropInService::class.java
        )
    }

    private fun filterOutGooglePay(paymentResponse: JSONObject): JSONObject {
        try {
            val filteredResponse = JSONObject(paymentResponse.toString())

            if (filteredResponse.has("paymentMethods")) {
                val paymentMethodsArray = filteredResponse.getJSONArray("paymentMethods")
                val filteredPaymentMethods = JSONArray()

                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    val type = paymentMethod.optString("type", "")

                    if (type != "googlepay" && type != "paywithgoogle") {
                        filteredPaymentMethods.put(paymentMethod)
                        Log.d("check_payment", "Included payment method: $type")
                    } else {
                        Log.d("check_payment", "Excluded Google Pay payment method: $type")
                    }
                }
                filteredResponse.put("paymentMethods", filteredPaymentMethods)
            }

            return filteredResponse

        } catch (e: Exception) {
            e.printStackTrace()
            return paymentResponse
        }
    }


    override fun onDropInResult(dropInResult: DropInResult?) {
        when (dropInResult) {
            // The payment finishes with a result.
            is DropInResult.Finished -> {

                Log.d("check_finish","finish")

                //////////////////
                val result = dropInResult.result
                val paymentResult = Gson().fromJson(result, PaymentDetailsResponse::class.java)
                Log.d("check_pay_details_res"," payment data -> ${paymentResult}")
                try {
                    if (paymentResult != null) {
                        Log.d("check_payment","result code -> ${paymentResult.resultCode}")

                        if (paymentResult.resultCode?.value.equals("Authorised")){
                            Log.d("check_payment","inside authorised block")
                            Toast.makeText(requireContext(),"Payment Successfull",Toast.LENGTH_LONG).show()
                        }
                        else{
                            Log.d("check_payment","inside else block")
                            Toast.makeText(requireContext(),"",Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(requireContext(),"Payment Successfull",Toast.LENGTH_LONG).show()
                    }
                } catch (e:Exception){

                    Log.d("check_error"," Error = ${e.message}")
                    Toast.makeText(requireContext(),"${e.message}",Toast.LENGTH_LONG).show()
                }

            }
            // The shopper dismisses Drop-in.
            is DropInResult.CancelledByUser ->{
                Log.d("check_cancel","canceled by the user")

                Toast.makeText(requireContext(),"canceled by the user",Toast.LENGTH_LONG).show()
            }
            // Drop-in encounters an error.
            is DropInResult.Error -> {
                Log.d("check_dropinerror"," drop in error ${dropInResult.reason}")
                Toast.makeText(requireContext(),"drop in error ${dropInResult.reason}",Toast.LENGTH_LONG).show()


            }
            // Drop-in encounters an unexpected state.
            null ->{
                Log.d("check_null","value null")
                Toast.makeText(requireContext(),"null case called",Toast.LENGTH_LONG).show()

            }
        }
    }

}
