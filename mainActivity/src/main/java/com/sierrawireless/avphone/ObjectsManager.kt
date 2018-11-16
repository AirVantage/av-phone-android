package com.sierrawireless.avphone


import android.content.Context
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.tools.MyPreference
import java.util.concurrent.CopyOnWriteArrayList

class ObjectsManager private constructor() {
    private var mainActivity: MainActivity? = null
    var current: Int = 0
    private var savedPosition = -1

    internal var objects: CopyOnWriteArrayList<AvPhoneObject>

    val savecObject: AvPhoneObject
        get() = objects[savedPosition]

    val savedObjectName: String?
        get() = objects[savedPosition].name

    val currentObject: AvPhoneObject?
        get() = if (objects.isEmpty()) {
            null
        } else {
            if (current > objects.size) {
                null
            }else {
                objects[current]
            }
        }

    init {
        objects = CopyOnWriteArrayList()
    }

    internal fun init(activity: MainActivity) {
        this.mainActivity = activity
        val pref = MyPreference(activity.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE))


        objects = pref.getListObject(MODELS, AvPhoneObject::class.java)
        current = pref.getInt(ACTIVE)

        if (objects.isEmpty()) {
            // Create the default model here
            val model = AvPhoneObject()
            model.name = "Printer"
            var data = AvPhoneObjectData("A6 Page Count", "page(s)", "0", AvPhoneObjectData.Mode.UP, "1", null)
            model.add(data)
            data = AvPhoneObjectData("A4 Page Count", "page(s)", "0", AvPhoneObjectData.Mode.UP, "2", null)
            model.add(data)
            data = AvPhoneObjectData("Black Cartridge S/N", "", "NTOQN-7HUL9-NEPFL-13IOA", AvPhoneObjectData.Mode.None, "3", null)
            model.add(data)
            data = AvPhoneObjectData("Black lnk Level", "%", "100", AvPhoneObjectData.Mode.DOWN, "4",null)
            model.add(data)
            data = AvPhoneObjectData("Color Cartridge S/N", "", "629U7-XLT5H-6SCGJ-@CENZ", AvPhoneObjectData.Mode.None, "5",null)
            model.add(data)
            data = AvPhoneObjectData("Color lnk Level", "%", "100", AvPhoneObjectData.Mode.DOWN, "6",null)
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

    fun saveOnPref() {
        val pref = MyPreference(mainActivity!!.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE))
        // Save the list for later
        pref.putListObject(MODELS, objects)
        pref.putInt(ACTIVE, current)
    }

    internal fun reload() {

        val pref = MyPreference(mainActivity!!.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE))


        objects = pref.getListObject(MODELS, AvPhoneObject::class.java)
        current = pref.getInt(ACTIVE)

    }

     fun changeCurrent(name: String) {
        for ((index, obj) in objects.withIndex()) {
            if (obj.name == name) {
                current = index
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
