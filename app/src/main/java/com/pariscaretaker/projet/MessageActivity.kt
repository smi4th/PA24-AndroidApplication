package com.pariscaretaker.projet

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.widget.Toast
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import java.io.InputStream
import java.util.Properties
import org.json.JSONArray


class MessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

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

        getUUID(object : UUIDCallback {
            override fun onUUIDReceived(uuid: String) {
                Log.d("UUID", uuid)
                // Use the uuid as needed
            }

            override fun onError(error: String) {
                Log.e("ERROR", error)
            }
        })
    }

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_token", null)
    }

    private fun getUUID(callback: UUIDCallback) {
        val localProperties = Properties()
        try {
            val inputStream: InputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show()
            callback.onError("local.properties file not found")
            return
        }
        val apiUrl = localProperties.getProperty("api.url")
        val token = getToken()
        if (token == null) {
            Toast.makeText(this, "User token not found", Toast.LENGTH_LONG).show()
            callback.onError("User token not found")
            return
        }
        val url = "$apiUrl/account?token=$token"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val dataArray = response.getJSONArray("data")
                val user = dataArray.getJSONObject(0)
                val uuid = user.getString("uuid")
                callback.onUUIDReceived(uuid)
            },
            { error ->
                callback.onError("Error occurred: ${error.message}")
            }
        )

        // Add the request to the RequestQueue
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(request)
    }

    private fun getMessagesByUuid(uuid: String) {
        val localProperties = Properties()
        try {
            val inputStream: InputStream = assets.open("local.properties")
            localProperties.load(inputStream)
        } catch (e: Exception) {
            Toast.makeText(this, "local.properties file not found", Toast.LENGTH_LONG).show()
            return
        }
        val apiUrl = localProperties.getProperty("api.url")
        val token = getToken()
        if (token == null) {
            Toast.makeText(this, "User token not found", Toast.LENGTH_LONG).show()
            return
        }
        val url = "$apiUrl/messages?uuid=$uuid"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val messageArray = response.getJSONArray("data")
                val messages = parseMessages(messageArray)
                // Update UI with messages
            },
            { error ->
                Log.e("API_ERROR", "Error fetching messages: ${error.message}")
                Toast.makeText(this, "Error fetching messages", Toast.LENGTH_LONG).show()
            }
        )

        // Add the request to the RequestQueue
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(request)
    }

private fun parseMessages(messageArray: JSONArray): List<Message> {
        val messages = mutableListOf<Message>()
        for (i in 0 until messageArray.length()) {
            val messageObject = messageArray.getJSONObject(i)
            val message = Message(
                uuid = messageObject.getString("uuid"),
                content = messageObject.getString("content"),
                time = messageObject.getString("time")
            )
            messages.add(message)
        }
        return messages
    }
}

@Parcelize
data class Message(
    val uuid: String,
    val content: String,
    val time: String
) : Parcelable

@Parcelize
data class Contact(
    val uuid: String,
    val name: String,
    val lastMessage: String,
    val time: String
) : Parcelable

interface UUIDCallback {
    fun onUUIDReceived(uuid: String)
    fun onError(error: String)
}
