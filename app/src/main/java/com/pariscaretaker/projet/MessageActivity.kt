package com.pariscaretaker.projet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.Properties

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

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_messages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        getUUID(object : UUIDCallback {
            override fun onUUIDReceived(uuid: String) {
                Log.d("UUID", uuid)
                fetchAllMessages(uuid) { messages ->
                    fetchUserInfo { users ->
                        val contacts = combineMessagesWithUsers(messages, users)
                        updateRecyclerView(contacts)
                    }
                }
            }

            override fun onError(error: String) {
                Log.e("ERROR", error)
            }
        })
    }

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("user_token", null)
        Log.d("TOKEN", "Token: $token")
        return token
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
        Log.d("API_URL", "URL: $url")

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val dataArray = response.getJSONArray("data")
                val user = dataArray.getJSONObject(0)
                val uuid = user.getString("uuid")
                callback.onUUIDReceived(uuid)
            },
            { error ->
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    Toast.makeText(this, "Unauthorized access. Please log in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    callback.onError("Error occurred: ${error.message}")
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

    private fun fetchAllMessages(uuid: String, callback: (List<Message>) -> Unit) {
        val messages = mutableListOf<Message>()
        getMessagesByUuid(uuid, "account") { recipientMessages ->
            messages.addAll(recipientMessages)
            getMessagesByUuid(uuid, "author") { authorMessages ->
                messages.addAll(authorMessages)
                callback(messages)
            }
        }
    }

    private fun getMessagesByUuid(uuid: String, type: String, callback: (List<Message>) -> Unit) {
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
        val url = "$apiUrl/message?$type=$uuid"

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
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

    private fun fetchUserInfo(callback: (List<User>) -> Unit) {
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
        val url = "$apiUrl/account?all=true"

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val userArray = response.getJSONArray("data")
                Log.d("API_RESPONSE", userArray.toString())
                val users = parseUsers(userArray)
                callback(users)
            },
            { error ->
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    Toast.makeText(this, "Unauthorized access. Please log in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    Log.e("API_ERROR", "Error fetching users: ${error.message}")
                    Toast.makeText(this, "Error fetching users", Toast.LENGTH_LONG).show()
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

    private fun combineMessagesWithUsers(messages: List<Message>, users: List<User>): List<Contact> {
        val userMap = users.associateBy { it.uuid }
        val contacts = messages.map { message ->
            val user = userMap[message.author]
            Contact(
                uuid = message.author,
                name = "${user?.firstName} ${user?.lastName}",
                lastMessage = message.content,
                time = message.creationDate
            )
        }.distinctBy { it.uuid }
        Log.d("COMBINE_MESSAGES", "Combined contacts: ${contacts.size}")
        contacts.forEach {
            Log.d("CONTACT_INFO", "Contact: ${it.name}, Last Message: ${it.lastMessage}")
        }
        return contacts
    }


    private fun updateRecyclerView(contacts: List<Contact>) {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_messages)
        val contactAdapter = recyclerView.adapter as? ContactAdapter
        if (contactAdapter != null) {
            contactAdapter.updateContacts(contacts)
        } else {
            recyclerView.adapter = ContactAdapter(contacts)
        }
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
        return messages
    }

    private fun parseUsers(userArray: JSONArray): List<User> {
        val users = mutableListOf<User>()
        for (i in 0 until userArray.length()) {
            val userObject = userArray.getJSONObject(i)
            val user = User(
                uuid = userObject.getString("uuid"),
                firstName = userObject.getString("first_name"),
                lastName = userObject.getString("last_name")
            )
            users.add(user)
        }
        return users
    }
}

@Parcelize
data class Message(
    val uuid: String,
    val creationDate: String,
    val content: String,
    val account: String,
    val author: String
) : Parcelable

@Parcelize
data class Contact(
    val uuid: String,
    val name: String,
    val lastMessage: String,
    val time: String
) : Parcelable

@Parcelize
data class User(
    val uuid: String,
    val firstName: String,
    val lastName: String
) : Parcelable

interface UUIDCallback {
    fun onUUIDReceived(uuid: String)
    fun onError(error: String)
}
