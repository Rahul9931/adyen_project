package com.example.payment_projecgt.fragment

// Adyen Java API Library v27.0.0

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.adyen.enums.Environment
import com.adyen.model.checkout.Amount
import com.adyen.model.checkout.PaymentMethodsRequest
import com.adyen.model.checkout.PaymentMethodsResponse
import com.adyen.service.checkout.PaymentsApi
import com.example.payment_projecgt.R
import com.example.payment_projecgt.constants.ApplicationConstant
import com.example.payment_projecgt.databinding.FragmentPaymentBinding
import com.example.payment_projecgt.service.YourDropInService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
            callPaymentMethod()
        }

        return binding.root
    }

    private fun callPaymentMethod() {
        val constant = ApplicationConstant()
        // For the live environment, additionally include your liveEndpointUrlPrefix.
        // For the live environment, additionally include your liveEndpointUrlPrefix.
        val client = Client(constant.API_KEY, Environment.LIVE)

        // Create the request object(s)
        val amount = Amount()
            .currency("EUR")
            .value(1000L)

        val paymentMethodsRequest = PaymentMethodsRequest()
            .amount(amount)
            .merchantAccount(constant.MERCHANT_ACCOUNT)
            .countryCode("NL")
            .channel(PaymentMethodsRequest.ChannelEnum.ANDROID)
            .shopperLocale("nl-NL")

        // Send the request
        val service = PaymentsApi(client,constant.BASE_URL)
        GlobalScope.launch {
            callApi(service,paymentMethodsRequest)
        }

    }

    private suspend fun callApi(service: PaymentsApi, paymentMethodsRequest: PaymentMethodsRequest) {

        val job = CoroutineScope(Dispatchers.IO).launch {
            val response = service.paymentMethods(paymentMethodsRequest)
            Log.d("check_res","${response}")
            paymentResponse = response
        }
        job.join()

        val gson = Gson()
        val jsonResponse = JSONObject(gson.toJsonTree(paymentResponse).asJsonObject.toString())
        paymentStart(jsonResponse)

    }

    private fun paymentStart(paymentResponse: JSONObject) {
        val paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(paymentResponse)
        Log.d("check_payapi","${paymentMethodsApiResponse}")
        Log.d("check_payapi","${dropInLauncher}")

        // Create the amount object.
        val amount = com.adyen.checkout.components.core.Amount(
            currency = "EUR",
            value = 1000,
        )

// Create a configuration object.
        val dropInConfiguration = DropInConfiguration.Builder(
            shopperLocale= Locale("en", "US"), // Use your context instead to use the device's default locale.
            environment= com.adyen.checkout.core.Environment.EUROPE,
            clientKey = ApplicationConstant().CLIENT_KEY
        )
// Set the amount on the Drop-in.
            .setAmount(amount) // Optional to show the amount on the Pay button.
            .build()

        // Create a configuration object.
//        val checkoutConfiguration = CheckoutConfiguration(
//            environment = com.adyen.checkout.core.Environment.EUROPE,
//            clientKey = ApplicationConstant().CLIENT_KEY,
//        ) {
//            // Configure 3D Secure 2.
//            adyen3DS2 {
//                setThreeDSRequestorAppURL("https://com.example.payment_projecgt/adyen3ds2") // Strongly recommended.
//            }
//        }

        DropIn.startPayment(
            requireContext(),
            dropInLauncher,
            paymentMethodsApiResponse,
            dropInConfiguration,
            YourDropInService::class.java
        )

    }

    override fun onDropInResult(dropInResult: DropInResult?) {
        when (dropInResult) {
            // The payment finishes with a result.
            is DropInResult.Finished -> {
                Log.d("check_finish","finish")
            }
            // The shopper dismisses Drop-in.
            is DropInResult.CancelledByUser ->{
                Log.d("check_cancel","cancel")
            }
            // Drop-in encounters an error.
            is DropInResult.Error -> {
                Log.d("check_error","error")
            }
            // Drop-in encounters an unexpected state.
            null ->{
                Log.d("check_null","value null")
            }
        }
    }

}