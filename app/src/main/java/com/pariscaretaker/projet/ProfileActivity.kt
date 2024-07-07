package com.pariscaretaker.projet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat



class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //colorie la barre de notif
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.buttonColor)
        }

        val profileArrow = findViewById<ImageView>(R.id.profile_arrow)
        profileArrow.setOnClickListener {
            startActivity(Intent(this, ShowProfileActivity::class.java))
        }

        val nfcArrow = findViewById<RelativeLayout>(R.id.nfc_scan)
        try {
            nfcArrow.setOnClickListener {
                startActivity(Intent(this, NfcActivity::class.java))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unavailable: $e.", Toast.LENGTH_LONG).show()
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

        val logoutBtn = findViewById<Button>(R.id.logout_btn)
        logoutBtn.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("user_token")
            apply()
        }
        Toast.makeText(this, "Logout sucessful", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
