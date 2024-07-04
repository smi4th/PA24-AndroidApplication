package com.pariscaretaker.projet

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.io.InputStream
import java.util.Properties

class ChatActivity : AppCompatActivity() {

    private lateinit var contact: Contact
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

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

        contact = intent.getParcelableExtra<Contact>("contact")!!
        title = contact.name

        recyclerView = findViewById(R.id.recycler_view_chat)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchMessagesForContact(contact.uuid) { messages ->
            updateRecyclerView(messages)
        }
    }

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_token", null)
    }

    private fun fetchMessagesForContact(contactUuid: String, callback: (List<Message>) -> Unit) {
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
        val url = "$apiUrl/message?account=$contactUuid&author=$contactUuid"
        Log.d("API_REQUEST", "Fetching messages from URL: $url with token: $token for contact UUID: $contactUuid")

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("FULL_API_RESPONSE", response.toString())
                val messageArray = response.getJSONArray("data")
                Log.d("API_RESPONSE", messageArray.toString())
                val messages = parseMessages(messageArray)
                callback(messages)
            },
            { error ->
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    Toast.makeText(this, "Unauthorized access. Please log in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    Log.e("API_ERROR", "Error fetching messages: ${error.message}")
                    Toast.makeText(this, "Error fetching messages", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(request)
    }

    private fun updateRecyclerView(messages: List<Message>) {
        Log.d("UPDATE_RECYCLER_VIEW", "Updating RecyclerView with ${messages.size} messages")
        val messageAdapter = MessageAdapter(messages, getToken())
        recyclerView.adapter = messageAdapter
        messageAdapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(messages.size - 1)
    }


    private fun parseMessages(messageArray: JSONArray): List<Message> {
        val messages = mutableListOf<Message>()
        for (i in 0 until messageArray.length()) {
            val messageObject = messageArray.getJSONObject(i)
            Log.d("PARSE_MESSAGE", messageObject.toString())
            val message = Message(
                uuid = messageObject.getString("uuid"),
                creationDate = messageObject.getString("creation_date"),
                content = messageObject.getString("content"),
                account = messageObject.getString("account"),
                author = messageObject.getString("author")
            )
            messages.add(message)
        }
        return messages.sortedBy { it.creationDate }
    }

}
