package com.example.nfc_demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityMainBinding
import com.example.nfc_demo.nfc.ReceiverActivity
import com.example.nfc_demo.nfc.desfire.WriterActivity
import com.example.nfc_demo.weigand.UsbWeigandActivity
import com.example.nfc_demo.weigand.WifiWiegandActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.writeNFC.setOnClickListener {
            val intent = Intent(this, WriterActivity::class.java)
            startActivity(intent)
        }
        binding.readNFC.setOnClickListener {
            val intent = Intent(this, ReceiverActivity::class.java)
            startActivity(intent)
        }
        binding.usbTxn.setOnClickListener {
            val intent = Intent(this, UsbWeigandActivity::class.java)
            startActivity(intent)
        }
        binding.wifiTxn.setOnClickListener {
            val intent = Intent(this, WifiWiegandActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.writeNFC.visibility = View.GONE
        binding.readNFC.visibility = View.GONE
        binding.usbTxn.visibility = View.GONE
        binding.wifiTxn.visibility = View.VISIBLE
    }


}