package com.example.nfc_demo.access_management

import android.widget.AdapterView
import android.widget.Spinner

fun Spinner.setOnItemSelectedListener(onItemSelected: (position: Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: android.view.View?,
            position: Int,
            id: Long
        ) {
            onItemSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}
