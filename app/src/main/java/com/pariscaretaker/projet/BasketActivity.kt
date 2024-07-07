package com.pariscaretaker.projet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.view.CardInputWidget
import org.json.JSONObject
import java.util.*
import android.util.Log

class BasketActivity : AppCompatActivity() {

    private lateinit var housing: Housing
    private lateinit var cardInputWidget: CardInputWidget
    private lateinit var payButton: Button
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var totalCost: Double = 0.0
    private lateinit var basketId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basket)

        val receivedIntent = intent
        Log.d("BasketActivity", "Received Intent: $receivedIntent")

        housing = intent.getParcelableExtra("housing")!!
        startDate = intent.getSerializableExtra("startDate") as Calendar
        endDate = intent.getSerializableExtra("endDate") as Calendar
        basketId = intent.getStringExtra("basketId")!!

        if (housing == null || startDate == null || endDate == null || basketId == null) {
            Toast.makeText(this, "Error: Missing reservation details", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d("BasketActivity", "Housing: $housing, StartDate: $startDate, EndDate: $endDate, BasketId: $basketId")


        val titleTextView = findViewById<TextView>(R.id.basket_housing_title)
        val priceTextView = findViewById<TextView>(R.id.basket_housing_price)
        val addressTextView = findViewById<TextView>(R.id.basket_housing_address)
        val imageView = findViewById<ImageView>(R.id.basket_housing_image)
        val nightsTextView = findViewById<TextView>(R.id.basket_housing_nights)
        val totalTextView = findViewById<TextView>(R.id.basket_housing_total)


        titleTextView.text = housing.title
        priceTextView.text = housing.price + " €"
        addressTextView.text = "${housing.street}, ${housing.zipCode} ${housing.city}"
        Glide.with(this).load(housing.imgPath).into(imageView)

        val numberOfNights = ((endDate!!.timeInMillis - startDate!!.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
        totalCost = numberOfNights * housing.price.toDouble()
        nightsTextView.text = "Number of nights: $numberOfNights"
        totalTextView.text = "Total cost: ${String.format("%.2f", totalCost)} €"

        cardInputWidget = findViewById(R.id.card_input_widget)
        payButton = findViewById(R.id.pay_button)

        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val privateKey = localProperties.getProperty("stripe.secret.key")
        val publicKey = localProperties.getProperty("stripe.public.key")

        PaymentConfiguration.init(
            applicationContext,
            publicKey
        )

        payButton.setOnClickListener {
            val params = cardInputWidget.paymentMethodCreateParams ?: return@setOnClickListener
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                params, privateKey
            )
            payButton.isEnabled = false
            processPayment(confirmParams)
        }
    }

    private fun processPayment(params: ConfirmPaymentIntentParams) {
        val stripe = Stripe(this, PaymentConfiguration.getInstance(applicationContext).publishableKey)
        stripe.confirmPayment(this, params)

        updatePaymentStatus()

    }

    private fun updatePaymentStatus() {
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val api_url = localProperties.getProperty("api.url")
        val url = "$api_url/basket?uuid=$basketId"
        val requestQueue = Volley.newRequestQueue(this)
        val token = getToken()

        val paymentDetails = JSONObject().apply {
            put("paid", "1")
        }

        val request = object : JsonObjectRequest(
            Request.Method.PUT, url, paymentDetails,
            Response.Listener { response ->
                runOnUiThread {
                    Toast.makeText(this, "Payment successful and reservation confirmed", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                showError("Error updating payment status")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
        val intent = Intent(this, MainActivity::class.java)
    }

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_token", null)
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
