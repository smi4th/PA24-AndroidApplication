package com.pariscaretaker.projet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.os.Build
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import android.view.View
import java.io.InputStream
import java.util.Properties


class ShowProfileActivity: AppCompatActivity() {

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_profile)

        //colorie la barre de notif
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.buttonColor)
        }

        val goBackSection = findViewById<LinearLayout>(R.id.goBackBtn);
        goBackSection.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        });

        firstNameEditText = findViewById(R.id.firstName)
        lastNameEditText = findViewById(R.id.lastName)
        emailEditText = findViewById(R.id.email)

        getProfile()
    }

    fun getProfile() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("user_token", "")
        val queue = Volley.newRequestQueue(this)
        val localProperties = Properties()
        try {
            val inputStream: InputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show()
        }
        val api_url = localProperties.getProperty("api.url")

        val url = "$api_url/account?token=$token"

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                Log.d("ProfileActivity", response.toString())

                val dataArray = response.getJSONArray("data")
                if (dataArray.length() > 0) {
                    val profileData = dataArray.getJSONObject(0)
                    val firstName = profileData.getString("first_name")
                    val lastName = profileData.getString("last_name")
                    val email = profileData.getString("email")

                    firstNameEditText.setText(firstName)
                    lastNameEditText.setText(lastName)
                    emailEditText.setText(email)

                    Log.d("ProfileActivity", "First Name: $firstName")
                    Log.d("ProfileActivity", "Last Name: $lastName")
                    Log.d("ProfileActivity", "Email: $email")
                }
            },
            Response.ErrorListener { error ->
                Log.e("ProfileActivity", error.toString())
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        queue.add(request)
    }
}