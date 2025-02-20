package com.example.nfc_demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.nfc_demo.databinding.ActivityMainBinding

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
    }

    override fun onResume() {
        super.onResume()
    }


}