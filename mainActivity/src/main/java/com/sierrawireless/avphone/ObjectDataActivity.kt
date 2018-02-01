package com.sierrawireless.avphone

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.model.AvPhoneObjectData
import kotlinx.android.synthetic.main.activity_object_data.*

class ObjectDataActivity : Activity(), AdapterView.OnItemSelectedListener {

    internal var objectsManager: ObjectsManager = ObjectsManager.getInstance()
    private var obj: AvPhoneObject? = null
    internal var data: AvPhoneObjectData? = null
    private var objectPosition: Int = 0
    private var dataPosition: Int = 0
    internal var add: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val menuList = arrayOf("None", "Increase indefinitely", "Decrease to zero")


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_data)
        val intent = intent
        objectPosition = intent.getIntExtra(ObjectConfigureActivity.OBJECT_POSITION, -1)
        dataPosition = intent.getIntExtra(ObjectConfigureActivity.DATA_POSITION, -1)
        add = intent.getBooleanExtra(ObjectConfigureActivity.ADD, true)

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

        } else {
            titleMenu.setText(R.string.add_new_data)
        }

        cancelData.setOnClickListener { finish() }

        saveData.setOnClickListener {
            val mode = AvPhoneObjectData.modeFromPosition(spinner.selectedItemPosition)
            if (add) {
                data = AvPhoneObjectData(
                        nameText.text.toString(),
                        unitText.text.toString(),
                        defaultText.text.toString(),
                        mode,
                        objectPosition.toString()
                )
                obj!!.datas.add(data!!)
            } else {
                data!!.unit = unitText.text.toString()
                data!!.defaults = defaultText.text.toString()
                data!!.mode = mode
            }
            finish()
        }

    }

    override fun onItemSelected(parent: AdapterView<*>, view: View,
                                pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

}
