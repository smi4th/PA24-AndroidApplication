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
import androidx.core.content.ContextCompat
import java.util.Properties
import java.io.InputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import com.android.volley.toolbox.HurlStack
import android.annotation.SuppressLint


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //colorie la barre de notif
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.headerBackground)
        }

        val usernameEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            loginUser(username, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        val localProperties = Properties()
        try {
            val inputStream: InputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show()
        }
        val api_url = localProperties.getProperty("api.url")

        val url = "$api_url/login"
        val params = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        Log.d("API_REQUEST_BODY", params.toString())

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, params,
            Response.Listener { response ->
                val token = response.getString("token")
                saveToken(token)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                Log.d("API_RESPONSE", response.toString())
            },
            Response.ErrorListener { error ->
                // Enhanced error logging
                error.printStackTrace()
                val responseBody = error.networkResponse?.data?.let { String(it) }
                val statusCode = error.networkResponse?.statusCode
                val errorMessage = error.message
                Log.e("API_ERROR", "Status Code: $statusCode, Error: $errorMessage, Response Body: $responseBody")
                Toast.makeText(this, "Login failed: $responseBody", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        val requestQueue = Volley.newRequestQueue(this, getHurlStack())
        requestQueue.add(request)
    }



    private fun saveToken(token: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_token", token)
            apply()
        }
    }

    @SuppressLint("TrustAllX509TrustManager")
    fun getHurlStack(): HurlStack {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        return HurlStack(null, sslSocketFactory)
    }

}
