package com.sierrawireless.avphone


import android.content.Context
import android.util.Log

import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.tools.MyPreference

import java.util.ArrayList

class ObjectsManager private constructor() {
    private var mainActivyty: MainActivity? = null
    var current: Int = 0
    private var savedPosition = -1

    internal var objects: ArrayList<AvPhoneObject>

    val savecObject: AvPhoneObject
        get() = objects[savedPosition]

    val savedObjectName: String?
        get() = objects[savedPosition].name

    val currentObject: AvPhoneObject?
        get() = if (objects.isEmpty()) {
            null
        } else objects[current]

    init {
        objects = ArrayList()
    }

    internal fun init(activity: MainActivity) {
        this.mainActivyty = activity
        val pref = MyPreference(activity.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE))


        objects = pref.getListObject(MODELS, AvPhoneObject::class.java)
        current = pref.getInt(ACTIVE)

        if (objects.isEmpty()) {
            // Create the default model here
            val model = AvPhoneObject()
            model.name = "Printer"
            var data = AvPhoneObjectData("A6 Page Count", "page(s)", "0", AvPhoneObjectData.Mode.UP, "1")
            model.add(data)
            data = AvPhoneObjectData("A4 Page Count", "page(s)", "0", AvPhoneObjectData.Mode.UP, "2")
            model.add(data)
            data = AvPhoneObjectData("Black Cartridge S/N", "", "NTOQN-7HUL9-NEPFL-13IOA", AvPhoneObjectData.Mode.None, "3")
            model.add(data)
            data = AvPhoneObjectData("Black lnk Level", "%", "100", AvPhoneObjectData.Mode.DOWN, "4")
            model.add(data)
            data = AvPhoneObjectData("Color Cartridge S/N", "", "629U7-XLT5H-6SCGJ-@CENZ", AvPhoneObjectData.Mode.None, "5")
            model.add(data)
            data = AvPhoneObjectData("Color lnk Level", "%", "100", AvPhoneObjectData.Mode.DOWN, "6")
            model.add(data)
            objects.add(model)
            current = 0
            saveOnPref()
        }
        if (savedPosition == -1) {
            savedPosition = current
        }
    }

    internal fun removeSavedObject() {
        objects.removeAt(savedPosition)
        savedPosition = current
        saveOnPref()
    }

    private fun saveOnPref() {
        val pref = MyPreference(mainActivyty!!.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE))
        // Save the list for later
        pref.putListObject(MODELS, objects)
        pref.putInt(ACTIVE, current)
    }

    internal fun reload() {

        val pref = MyPreference(mainActivyty!!.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE))


        objects = pref.getListObject(MODELS, AvPhoneObject::class.java)
        current = pref.getInt(ACTIVE)

    }

    fun execOnCurrent() {
        val obj = objects[current]
        obj.exec()
        // Save the list for later
        saveOnPref()
    }

     fun changeCurrent(name: String) {
        Log.d(TAG, "changeCurrent: change axctive to $name current before $current")
        for ((indice, obj) in objects.withIndex()) {
            if (obj.name == name) {
                current = indice
                Log.d(TAG, "changeCurrent: current after " + current)
                saveOnPref()
                return
            }
        }
    }

    internal fun setSavedPosition(position: Int) {
        savedPosition = position
    }

    internal fun getObjectByIndex(position: Int): AvPhoneObject? {
        return if (position > objects.size) {
            null
        } else objects[position]
    }

    fun getObjectByName(name: String): AvPhoneObject? {
        return objects.firstOrNull { it.name == name }
    }

    internal fun save() {
        saveOnPref()

    }

    companion object {
        private const val TAG = "ObjectsManager"
        private var lInstance:ObjectsManager? = null
        private const val SHARED_PREFS_FILE = "SavedModels"
        private const val MODELS = "models"
        private const val ACTIVE = "active"


        fun getInstance(): ObjectsManager {
            if (lInstance == null) {
                lInstance = ObjectsManager()
            }
            return lInstance!!
        }
    }


}