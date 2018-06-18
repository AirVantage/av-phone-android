package com.sierrawireless.avphone.activity

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.tools.Tools
import kotlinx.android.synthetic.main.activity_object_data.*
import org.jetbrains.anko.alert

class ObjectDataActivity : Activity(), AdapterView.OnItemSelectedListener {

    internal var objectsManager: ObjectsManager = ObjectsManager.getInstance()
    private var obj: AvPhoneObject? = null
    internal var data: AvPhoneObjectData? = null
    private var objectPosition: Int = 0
    private var dataPosition: Int = 0
    private lateinit var objname: String
    internal var add: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val menuList = arrayOf("None", "Increase indefinitely", "Decrease to zero", "Random")


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_data)
        val intent = intent
        objectPosition = intent.getIntExtra(ObjectConfigureActivity.OBJECT_POSITION, -1)
        dataPosition = intent.getIntExtra(ObjectConfigureActivity.DATA_POSITION, -1)
        add = intent.getBooleanExtra(ObjectConfigureActivity.ADD, true)
        objname = intent.getStringExtra(ObjectConfigureActivity.OBJECT_NAME)

        if (objectPosition == -1 || dataPosition == -1) {
            return
        }


        objectsManager = ObjectsManager.getInstance()
        obj = objectsManager.getObjectByIndex(objectPosition)

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, menuList)
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner

        spinner.adapter = adapter
        spinner.onItemSelectedListener = this

        if (!add) {
            name.visibility = View.INVISIBLE
            nameText.visibility = View.GONE
            data = obj!!.datas[dataPosition]
            titleMenu.text = data!!.name
            unitText.setText(data!!.unit)
            defaultText.setText(data!!.defaults)
            spinner.setSelection(data!!.modePosition(), false)
            if (data!!.path != null) {
                path.setText(data!!.path)
            }else{
                path.setText(Tools.buildDefaultPath(objname, dataPosition))
               // path.setText("default")
            }
        } else {
            titleMenu.setText(R.string.add_new_data)
            path.setText(Tools.buildDefaultPath(objname, dataPosition))
        }

        cancelData.setOnClickListener { finish() }

        saveData.setOnClickListener {
            val mode = AvPhoneObjectData.modeFromPosition(spinner.selectedItemPosition)
            if (add) {
                if (!validateEntry()) return@setOnClickListener
                data = if (!path.text.isEmpty() && path.text.toString() == "default") {
                    AvPhoneObjectData(
                            nameText.text.toString(),
                            unitText.text.toString(),
                            defaultText.text.toString(),
                            mode, "1", null)
                }else {
                    AvPhoneObjectData(
                            nameText.text.toString(),
                            unitText.text.toString(),
                            defaultText.text.toString(),
                            mode, "1",
                            path.text.toString())
                }
                obj!!.datas.add(data!!)
            } else {
                if (!validateEntry()) return@setOnClickListener
                data!!.unit = unitText.text.toString()
                data!!.defaults = defaultText.text.toString()
                data!!.mode = mode
                if (!path.text.isEmpty() && path.text.toString() != "default") {
                    data!!.path = path.text.toString()
                }else{
                    data!!.path = null
                }
            }

            finish()
        }

    }

    private fun validateEntry():Boolean {
        val mode = AvPhoneObjectData.modeFromPosition(spinner.selectedItemPosition)
        if (add) {
            if (nameText.text.toString().isEmpty()) {
                alert(getString(R.string.nameOnbjectDataEmpty), getString(R.string.alert)) {
                    positiveButton("OK") {
                    }
                }.show()
                return false
            }
        }
        if (mode == AvPhoneObjectData.Mode.RANDOM) {
            // Is this case we must verify the default entry
            val defaults = defaultText.text.toString().split(",")
            if (defaults.size != 2) {
                alert(getString(R.string.InvalidRandomDefaultComma), getString(R.string.alert)) {
                    positiveButton("OK") {
                    }
                }.show()
                return false
            }
            if ((!(!defaults[0].isEmpty() && TextUtils.isDigitsOnly(defaults[0]))) or
                    (!(!defaults[1].isEmpty() && TextUtils.isDigitsOnly(defaults[1])))) {
                alert(
                        getString(R.string.MinMaxRandomDataObject),
                        getString(R.string.alert)
                ) {
                    positiveButton("OK") {
                    }
                }.show()
                return false
            }
        }
        if (mode == AvPhoneObjectData.Mode.DOWN || mode == AvPhoneObjectData.Mode.UP) {
            // check the default value must be a number
            if (!(!defaultText.text.toString().isEmpty() && TextUtils.isDigitsOnly(defaultText.text.toString()))) {
                alert(getString(R.string.InvalidDefaultUpDown), getString(R.string.alert)) {
                    positiveButton("OK") {
                    }
                }.show()
                return false
            }
        }
        return true
    }

    override fun onBackPressed() = finish()

    override fun onItemSelected(parent: AdapterView<*>, view: View?,
                                pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

}
