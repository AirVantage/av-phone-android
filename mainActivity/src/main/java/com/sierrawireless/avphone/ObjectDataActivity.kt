package com.sierrawireless.avphone

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.model.AvPhoneObjectData

class ObjectDataActivity : Activity(), AdapterView.OnItemSelectedListener {
    internal lateinit var title: TextView
    internal lateinit var name: TextView
    private lateinit var nameEdit: EditText
    private lateinit var unitEdit: EditText
    private lateinit var defaultEdit: EditText
    private lateinit var simulationSpin: Spinner
    private lateinit var saveBtn: Button
    private lateinit var cancelBtn: Button
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
        title = findViewById(R.id.title)
        name = findViewById(R.id.name)
        nameEdit = findViewById(R.id.nameText)
        unitEdit = findViewById(R.id.unitText)
        defaultEdit = findViewById(R.id.defaultText)
        simulationSpin = findViewById(R.id.spinner)
        saveBtn = findViewById(R.id.saveData)
        cancelBtn = findViewById(R.id.cancelData)

        objectsManager = ObjectsManager.getInstance()
        obj = objectsManager.getObjectByIndex(objectPosition)

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, menuList)
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner

        simulationSpin.adapter = adapter
        simulationSpin.onItemSelectedListener = this

        if (!add) {
            name.visibility = View.INVISIBLE
            nameEdit.visibility = View.GONE
            data = obj!!.datas[dataPosition]
            title.text = data!!.name
            unitEdit.setText(data!!.unit)
            defaultEdit.setText(data!!.defaults)
            simulationSpin.setSelection(data!!.modePosition(), false)

        } else {
            title.setText(R.string.add_new_data)
        }

        cancelBtn.setOnClickListener { finish() }

        saveBtn.setOnClickListener {
            val mode = AvPhoneObjectData.modeFromPosition(simulationSpin.selectedItemPosition)
            if (add) {
                data = AvPhoneObjectData(
                        nameEdit.text.toString(),
                        unitEdit.text.toString(),
                        defaultEdit.text.toString(),
                        mode,
                        objectPosition.toString()
                )
                obj!!.datas.add(data!!)
            } else {
                data!!.unit = unitEdit.text.toString()
                data!!.defaults = defaultEdit.text.toString()
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
