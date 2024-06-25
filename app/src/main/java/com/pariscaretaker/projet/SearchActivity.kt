package com.pariscaretaker.projet

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.android.volley.Response
import org.json.JSONObject
import java.io.InputStream
import java.util.Properties
import com.android.volley.RequestQueue
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.android.volley.toolbox.StringRequest
import com.android.volley.VolleyError


class SearchActivity : AppCompatActivity() {

    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var locationAutoCompleteTextView: AutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var placesClient: PlacesClient
    private val startDateCalendar: Calendar = Calendar.getInstance()
    private val endDateCalendar: Calendar = Calendar.getInstance()
    private var selectedPlaceId: String? = null
    private var selectedLatLng: LatLng? = null

    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.headerBackground)
        }

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

        requestQueue = Volley.newRequestQueue(this)


        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        locationAutoCompleteTextView = findViewById(R.id.locationAutoCompleteTextView)
        searchButton = findViewById(R.id.searchButton)

        startDateEditText.setOnClickListener {
            showDatePickerDialog(startDateEditText, startDateCalendar)
        }

        endDateEditText.setOnClickListener {
            showDatePickerDialog(endDateEditText, endDateCalendar)
        }

        //autocompletion
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        locationAutoCompleteTextView.setAdapter(adapter)

        locationAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val token = AutocompleteSessionToken.newInstance()
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(token)
                    .setQuery(s.toString())
                    .build()

                placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                    val predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                    adapter.clear()
                    adapter.addAll(predictions)
                    adapter.notifyDataSetChanged()
                    locationAutoCompleteTextView.showDropDown()

                    if (response.autocompletePredictions.isNotEmpty()) {
                        selectedPlaceId = response.autocompletePredictions[0].placeId
                    }
                }
            }
        })

        searchButton.setOnClickListener {
            selectedPlaceId?.let { placeId ->
                val placeFields = listOf(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.LAT_LNG)
                val request = FetchPlaceRequest.newInstance(placeId, placeFields)

                placesClient.fetchPlace(request).addOnSuccessListener { response ->
                    val place = response.place
                    selectedLatLng = place.latLng

                    selectedLatLng?.let { latLng ->
                        fetchHousingData(latLng) { housingList ->
                            Toast.makeText(this, "Found ${housingList.size} housings", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, MapActivity::class.java)
                            intent.putExtra("LATITUDE", latLng.latitude)
                            intent.putExtra("LONGITUDE", latLng.longitude)
                            intent.putParcelableArrayListExtra("HOUSING_LIST", ArrayList(housingList))
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }


    private fun fetchHousingData(latLng: LatLng, callback: (List<Housing>) -> Unit) {
        val localProperties = Properties()
        try {
            val inputStream: InputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show()
            return
        }
        val api_url = localProperties.getProperty("api.url")

        val url = "$api_url/housing?all=true"
        val token = getToken()

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d("API_RESPONSE", response.toString())
                try {
                    parseHousingData(response) { housingList ->
                        if (housingList.isNullOrEmpty()) {
                            showError("No data available")
                        } else {
                            callback(housingList)
                        }
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

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_token", null)
    }

    private fun parseHousingData(response: JSONObject, callback: (List<Housing>) -> Unit) {
        val housingList = mutableListOf<Housing>()
        if (!response.has("data")) {
            throw JSONException("No value for data")
        }
        val jsonArray = response.getJSONArray("data")

        val addressList = mutableListOf<Pair<JSONObject, String>>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val city = jsonObject.getString("city")
            val zipCode = jsonObject.getString("zip_code")
            val street = jsonObject.getString("street")
            val address = "$street, $zipCode, $city"
            addressList.add(Pair(jsonObject, address))
        }

        // Function to handle the coordinates fetched for each address
        val handleFetchedCoordinates: (Int) -> Unit = { index ->
            if (index == addressList.size) {
                callback(housingList)
            }
        }

        addressList.forEachIndexed { index, (jsonObject, address) ->
            fetchCoordinates(address) { latLng ->
                val housing = Housing(
                    uuid = jsonObject.getString("uuid"),
                    surface = jsonObject.getString("surface"),
                    price = jsonObject.getString("price"),
                    validated = jsonObject.getString("validated"),
                    streetNb = jsonObject.getString("street_nb"),
                    city = jsonObject.getString("city"),
                    zipCode = jsonObject.getString("zip_code"),
                    street = jsonObject.getString("street"),
                    description = jsonObject.getString("description"),
                    houseType = jsonObject.getString("house_type"),
                    account = jsonObject.getString("account"),
                    imgPath = jsonObject.optString("imgPath", "NULL"),
                    title = jsonObject.getString("title"),
                    latitude = latLng?.latitude ?: 0.0,
                    longitude = latLng?.longitude ?: 0.0
                )
                housingList.add(housing)
                handleFetchedCoordinates(index + 1)
            }
        }
    }

    private fun fetchCoordinates(address: String, callback: (LatLng?) -> Unit) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "%20")}&key=$apiKey"

        val request = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val results = jsonResponse.getJSONArray("results")
                    if (results.length() > 0) {
                        val location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                        val latLng = LatLng(location.getDouble("lat"), location.getDouble("lng"))
                        callback(latLng)
                    } else {
                        callback(null)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    callback(null)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                callback(null)
            })

        requestQueue.add(request)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showDatePickerDialog(dateEditText: EditText, calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            dateEditText.setText(getString(R.string.date_format, dayOfMonth, month + 1, year))
        }, year, month, day)
        datePickerDialog.show()
    }
}
