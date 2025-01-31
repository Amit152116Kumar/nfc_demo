package com.example.nfc_demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.nfc_demo.databinding.ActivitySentBinding

class SentActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySentBinding
    private val nfcReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {


            if (intent.hasExtra("auth")) {
                val value = intent.getStringExtra("auth")

                runOnUiThread {
                    binding.txtSent.text = value
                    Toast.makeText(applicationContext, value, Toast.LENGTH_SHORT).show()
                    binding.btn.visibility = View.VISIBLE

                }
            } else if (intent.hasExtra("read")) {
                val value = intent.getStringExtra("read")

                runOnUiThread {
                    binding.txtSent.text = value
                    binding.btn.visibility = View.GONE
                }
            }

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intentFilter = IntentFilter("HOST_APDU_SERVICE")
        LocalBroadcastManager.getInstance(this).registerReceiver(nfcReceiver, intentFilter)


        binding.btn.setOnClickListener {
            ApduProto.accept = true
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nfcReceiver)

    }
}