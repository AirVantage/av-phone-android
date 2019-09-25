package com.sierrawireless.avphone.tools

import android.content.SharedPreferences
import android.text.TextUtils
import com.google.gson.Gson
import com.sierrawireless.avphone.model.AvPhoneObject
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class MyPreference(private val preferences: SharedPreferences) {

    /**
     * Get String value from SharedPreferences at 'key'. If key not found, return ""
     * @param key SharedPreferences key
     * @return String value at 'key' or "" (empty String) if key not found
     */
    fun getString(key: String): String {


        return preferences.getString(key, "")!!
    }

    /**
     * Get parsed ArrayList of String from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * @return ArrayList of String
     */
    private fun getListString(key: String): ArrayList<String> {
        return ArrayList(listOf(*TextUtils.split(preferences.getString(key, ""), "‚‗‚")))
    }

    fun getListObject(key: String, mClass: Class<AvPhoneObject>): CopyOnWriteArrayList<AvPhoneObject> {
        val gson = Gson()

        val objStrings = getListString(key)
        return objStrings.mapTo(CopyOnWriteArrayList()) { gson.fromJson(it, mClass) }

    }


    /**
     * Get int value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * @return int value at 'key' or 'defaultValue' if key not found
     */
    fun getInt(key: String): Int {
        return preferences.getInt(key, 0)
    }

    /**
     * Put int value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param value int value to be added
     */
    fun putInt(key: String, value: Int) {
        checkForNullKey(key)
        preferences.edit().putInt(key, value).commit()
    }

    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     * @param key pref key
     */
    private fun checkForNullKey(key: String?) {
        if (key == null) {
            throw NullPointerException()
        }
    }

    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param stringList ArrayList of String to be added
     */
    private fun putListString(key: String, stringList: CopyOnWriteArrayList<String>) {
        checkForNullKey(key)
        val myStringList = stringList.toTypedArray()
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).commit()
    }

    fun putListObject(key: String, objArray: CopyOnWriteArrayList<AvPhoneObject>) {
        checkForNullKey(key)
        val gson = Gson()
        val objStrings = objArray.mapTo(CopyOnWriteArrayList<String>()) { gson.toJson(it) }
        putListString(key, objStrings)
    }
}
