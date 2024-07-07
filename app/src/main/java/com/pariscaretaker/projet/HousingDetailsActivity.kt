package com.pariscaretaker.projet

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class HousingDetailActivity : AppCompatActivity() {

    private lateinit var housing: Housing
    private lateinit var calendarView: CalendarView
    private lateinit var reservationInfo: TextView
    private lateinit var reserveButton: Button
    private val reservedDates = mutableListOf<EventDay>()
    private val selectedDates = mutableListOf<Calendar>()
    private var currentMonth: Int = -1
    private var currentYear: Int = -1
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_housing_detail)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.headerBackground)
        }

        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }

        housing = intent.getParcelableExtra("housing")!!

        val titleTextView = findViewById<TextView>(R.id.housing_detail_title)
        val priceTextView = findViewById<TextView>(R.id.housing_detail_price)
        val addressTextView = findViewById<TextView>(R.id.housing_detail_address)
        val descriptionTextView = findViewById<TextView>(R.id.housing_detail_description)
        val imageView = findViewById<ImageView>(R.id.housing_detail_image)
        calendarView = findViewById(R.id.calendarView)
        reservationInfo = findViewById(R.id.reservation_info)
        reserveButton = findViewById(R.id.reserve_button)

        titleTextView.text = housing.title
        priceTextView.text = housing.price + " €"
        addressTextView.text = "${housing.street}, ${housing.zipCode} ${housing.city}"
        descriptionTextView.text = housing.description

        if (housing.imgPath != "NULL") {
            Glide.with(this).load(housing.imgPath).into(imageView)
        }

        // Ajouter les icônes de navigation
        val profileIcon = findViewById<ImageView>(R.id.icon_home)
        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        val flightIcon = findViewById<ImageView>(R.id.icon_flight)
        flightIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        val homeIcon = findViewById<ImageView>(R.id.icon_search)
        homeIcon.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        val messageIcon = findViewById<ImageView>(R.id.icon_message)
        messageIcon.setOnClickListener {
            startActivity(Intent(this, MessageActivity::class.java))
        }

        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                val selectedDate = eventDay.calendar
                if (startDate == null || (startDate != null && endDate != null)) {
                    startDate = selectedDate
                    endDate = null
                    selectedDates.clear()
                    selectedDates.add(startDate!!)
                    reservationInfo.text = "Date de début sélectionnée: ${SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(startDate!!.time)}"
                } else if (startDate != null && endDate == null) {
                    if (selectedDate.before(startDate)) {
                        startDate = selectedDate
                        selectedDates.clear()
                        selectedDates.add(startDate!!)
                        reservationInfo.text = "Date de début sélectionnée: ${SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(startDate!!.time)}"
                    } else {
                        endDate = selectedDate
                        selectedDates.clear()
                        selectedDates.add(startDate!!)
                        val current = startDate!!.clone() as Calendar
                        while (current.before(endDate) || current == endDate) {
                            selectedDates.add(current.clone() as Calendar)
                            current.add(Calendar.DAY_OF_MONTH, 1)
                        }
                        val numberOfDays = ((endDate!!.timeInMillis - startDate!!.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
                        val totalCost = numberOfDays * housing.price.toDouble()
                        reservationInfo.text = "Je veux réserver du ${SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(startDate!!.time)} au ${SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(endDate!!.time)} pour un total de ${String.format("%.2f", totalCost)} €"
                    }
                }
                updateCalendarView()
            }
        })

        reserveButton.setOnClickListener {
            if (startDate != null && endDate != null) {
                checkAndCreateBasket()
            } else {
                Toast.makeText(this, "Veuillez sélectionner une date de début et une date de fin", Toast.LENGTH_LONG).show()
            }
        }

        currentMonth = calendarView.currentPageDate.get(Calendar.MONTH)
        currentYear = calendarView.currentPageDate.get(Calendar.YEAR)
        fetchAvailabilityForMonth(calendarView.currentPageDate)
    }

    private fun updateCalendarView() {
        val events = mutableListOf<EventDay>()
        val greenDrawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.green_dot)
        for (date in selectedDates) {
            greenDrawable?.let {
                events.add(EventDay(date, it))
            }
        }
        events.addAll(reservedDates)
        calendarView.setEvents(events)
    }

    private fun fetchAvailabilityForMonth(date: Calendar) {
        reservedDates.clear()
        val monthStart = date.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        val monthEnd = monthStart.clone() as Calendar
        monthEnd.add(Calendar.MONTH, 1)
        monthEnd.add(Calendar.DAY_OF_MONTH, -1)

        val current = monthStart.clone() as Calendar
        while (!current.after(monthEnd)) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(current.time)
            checkAvailabilityForDate(dateStr)
            current.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun checkAvailabilityForDate(dateStr: String) {
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val api_url = localProperties.getProperty("api.url")

        val url = "$api_url/housing/available?start_time=$dateStr&end_time=$dateStr&housing=${housing.uuid}"
        val requestQueue = Volley.newRequestQueue(this)
        val token = getToken()

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d("API_RESPONSE", response.toString())
                try {
                    if (!response.getBoolean("available")) {
                        markDateAsReserved(dateStr)
                    }
                } catch (e: JSONException) {
                    Log.e("JSON_ERROR", "Error parsing data: ${e.message}")
                    showError("Error parsing data")
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Log.e("API_ERROR", "Error fetching data: ${error.message}")
                showError("Error fetching data")
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun markDateAsReserved(dateStr: String) {
        val reservedDrawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.red_dot)
        if (reservedDrawable == null) {
            showError("Error loading drawable")
            return
        }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        date?.let {
            val calendar = Calendar.getInstance()
            calendar.time = it
            reservedDates.add(EventDay(calendar, reservedDrawable))
            updateCalendarView()
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            Log.d("HousingDetailActivity", message)
        }
    }

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_token", null)
    }

    private fun removeHousingFromBasket(basketId: String, housingId: String) {
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val api_url = localProperties.getProperty("api.url")
        val url = "$api_url/basket/housing?basket=$basketId&housing=$housingId"
        val requestQueue = Volley.newRequestQueue(this)
        val token = getToken()

        val request = object : JsonObjectRequest(
            Request.Method.DELETE, url, null,
            Response.Listener { response ->
                Log.d("API_RESPONSE", response.toString())
                addHousingToBasket(basketId)
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Log.e("API_ERROR", "Error removing housing from basket: ${error.message}")
                showError("Error removing housing from basket")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun getAccountUuid(callback: (String?) -> Unit) {
        val token = getToken() ?: return callback(null)
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return callback(null)
        }
        val api_url = localProperties.getProperty("api.url")

        val url = "$api_url/account?token=$token"
        val requestQueue = Volley.newRequestQueue(this)

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                Log.d("HousingDetailActivity", response.toString())

                val dataArray = response.getJSONArray("data")
                if (dataArray.length() > 0) {
                    val accountData = dataArray.getJSONObject(0)
                    val accountUuid = accountData.getString("uuid")
                    callback(accountUuid)
                } else {
                    callback(null)
                }
            },
            Response.ErrorListener { error ->
                Log.e("HousingDetailActivity", error.toString())
                callback(null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun checkAndCreateBasket() {
        getAccountUuid { accountUuid ->
            if (accountUuid != null) {
                deleteExistingBasket(accountUuid) {
                    createBasket(accountUuid)
                }
            } else {
                showError("Account UUID not found")
            }
        }
    }

    private fun deleteExistingBasket(accountUuid: String, callback: () -> Unit) {
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val api_url = localProperties.getProperty("api.url")
        val url = "$api_url/basket?account=$accountUuid"
        val requestQueue = Volley.newRequestQueue(this)
        val token = getToken()

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d("API_RESPONSE", response.toString())
                try {
                    if (response.has("count") && response.getInt("count") > 0) {
                        val basketId = response.getJSONArray("baskets").getJSONObject(0).getString("uuid")
                        removeBasket(basketId, callback)
                    } else {
                        callback()
                    }
                } catch (e: JSONException) {
                    Log.e("JSON_ERROR", "Error parsing data: ${e.message}")
                    showError("Error parsing data")
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Log.e("API_ERROR", "Error fetching basket: ${error.message}")
                showError("Error fetching basket")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun removeBasket(basketId: String, callback: () -> Unit) {
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

        val request = object : JsonObjectRequest(
            Request.Method.DELETE, url, null,
            Response.Listener { response ->
                Log.d("API_RESPONSE", response.toString())
                callback()
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Log.e("API_ERROR", "Error deleting basket: ${error.message}")
                showError("Error deleting basket")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun createBasket(accountUuid: String) {
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val api_url = localProperties.getProperty("api.url")
        val url = "$api_url/basket"
        val requestQueue = Volley.newRequestQueue(this)
        val token = getToken()

        val basketDetails = JSONObject().apply {
            put("account", accountUuid)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, basketDetails,
            Response.Listener { response ->
                val basketId = response.getString("uuid")
                addHousingToBasket(basketId)
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Log.e("API_ERROR", "Error creating basket: ${error.message}")
                showError("Error creating basket")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun addHousingToBasket(basketId: String) {
        val localProperties = Properties()
        try {
            val inputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show() }
            return
        }
        val api_url = localProperties.getProperty("api.url")
        val url = "$api_url/basket/housing"
        val requestQueue = Volley.newRequestQueue(this)
        val token = getToken()

        val reservationDetails = JSONObject().apply {
            put("basket", basketId)
            put("housing", housing.uuid)
            put("start_time", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate!!.time))
            put("end_time", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate!!.time))
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, reservationDetails,
            Response.Listener { response ->
                runOnUiThread {
                    Toast.makeText(this, "Réservation ajoutée au panier", Toast.LENGTH_LONG).show()
                    startPaymentActivity(basketId)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Log.e("API_ERROR", "Error adding housing to basket: ${error.message}")

                val responseBody = error.networkResponse?.data?.let {
                    String(it, Charsets.UTF_8)
                }

                Log.e("API_ERROR", "Response body: $responseBody")
                showError("Error adding housing to basket: $responseBody")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun startPaymentActivity(basketId: String) {
        val intent = Intent(this, BasketActivity::class.java).apply {
            putExtra("housing", housing)
            putExtra("startDate", startDate)
            putExtra("endDate", endDate)
            putExtra("basketId", basketId)
        }
        startActivity(intent)
    }

}
