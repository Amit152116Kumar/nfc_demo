package com.example.nfc_demo

import android.content.Context
import android.content.SharedPreferences

class SharedPref {

    val PREFS_NAME = "CBDC_Pref"

    fun savePreferences(context: Context, key: String?, value: String?): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit() // edit() fun is used save the data into sharedPreferences
        editor.putString(key, value)
        editor.apply()
        return key
    }

    fun loadPreferences(context: Context, key: String?): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, "")
    }
}