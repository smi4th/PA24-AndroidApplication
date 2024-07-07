package com.pariscaretaker.projet

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Build
import android.widget.ImageView

class NfcActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var textView: TextView
    private lateinit var dotLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        }


        textView = findViewById(R.id.scan)
        dotLayout = findViewById(R.id.animationLayout)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val dot1 = findViewById<TextView>(R.id.dot1)
        val dot2 = findViewById<TextView>(R.id.dot2)
        val dot3 = findViewById<TextView>(R.id.dot3)

        val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink)

        dot1.startAnimation(blinkAnimation)
        dot2.startAnimation(blinkAnimation)
        dot3.startAnimation(blinkAnimation)


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
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val intentFilter = arrayOf<IntentFilter>()

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilter, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == it.action ||
                NfcAdapter.ACTION_TECH_DISCOVERED == it.action ||
                NfcAdapter.ACTION_TAG_DISCOVERED == it.action) {

                val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                tag?.let { nfcTag ->
                    dotLayout.visibility = View.GONE
                    textView.text = ""
                    val techList = nfcTag.techList.joinToString(", ")
                    textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    val ndef = Ndef.get(nfcTag)
                    if (ndef != null) {
                        ndef.connect()
                        val ndefMessage = ndef.cachedNdefMessage
                        if (ndefMessage != null) {
                            for (record in ndefMessage.records) {
                                val payload = record.payload
                                val text = String(payload, Charsets.UTF_8).substring(3)
                                textView.append("Code NFC: $text\n")
                            }
                        } else {
                            textView.append("Erreur: code nfc vide\n")
                        }
                        ndef.close()
                    } else {
                        textView.append("Ce tage ne contient pas de code valide\n")
                    }
                }
            }
        }
    }
}
