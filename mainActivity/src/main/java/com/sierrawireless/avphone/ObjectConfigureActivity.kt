package com.sierrawireless.avphone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.sierrawireless.avphone.adapter.ObjectDataAdapter
import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.tools.Tools
import kotlinx.android.synthetic.main.activity_object_configure.*
import java.util.*

class ObjectConfigureActivity : Activity() {


    internal var objectsManager: ObjectsManager = ObjectsManager.getInstance()
    private var menu: ArrayList<String> = ArrayList()
    private var position: Int = 0
    private var obj: AvPhoneObject? = null

    private var context: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_configure)

        objectsManager = ObjectsManager.getInstance()

        context = this
        val creator = SwipeMenuCreator { menu ->
            when (menu.viewType) {
                0 -> {
                    // create "delete" item
                    val deleteItem = SwipeMenuItem(
                            applicationContext)
                    // set item background
                    deleteItem.background = ColorDrawable(Color.rgb(0xF9,
                            0x3F, 0x25))
                    // set item width
                    deleteItem.width = Tools.dp2px(context!!).toInt()
                    // set a icon
                    deleteItem.setIcon(android.R.drawable.ic_menu_delete)
                    // add to menu
                    menu.addMenuItem(deleteItem)
                }
            }
        }

        listView.setMenuCreator(creator)


        val intent = intent
        position = intent.getIntExtra(ConfigureFragment.INDEX, -1)

        if (position == -1) {
            obj = AvPhoneObject()
            titleObject.setText(R.string.add_new_object)
            objectsManager.objects.add(obj!!)
            position = objectsManager.objects.size - 1
        } else {
            obj = objectsManager.getObjectByIndex(position)
            nameObject.visibility = View.GONE
            objectNameEdit.visibility = View.GONE
            titleObject.text = obj!!.name
        }
        menuGeneration()

        cancel.setOnClickListener {
            //reload object from list
            objectsManager.reload()
            val i = Intent()
            setResult(Activity.RESULT_CANCELED, i)
            finish()
        }

        save.setOnClickListener {
            if (objectNameEdit.visibility != View.GONE) {
                obj!!.name = objectNameEdit.text.toString()
            }
            objectsManager.save()
            val i = Intent()
            i.putExtra(ConfigureFragment.POS, position)
            setResult(Activity.RESULT_OK, i)
            finish()
        }


        listView.setOnMenuItemClickListener { position, _, index ->
            when (index) {
                0 -> {
                    //delete
                    obj!!.datas.removeAt(position)
                    menuGeneration()
                }
            }
            false
        }
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, i, _ ->
            val lIntent = Intent(view.context, ObjectDataActivity::class.java)
            lIntent.putExtra(OBJECT_POSITION, position)
            lIntent.putExtra(DATA_POSITION, i)
            if (i == menu.size - 1) {
                lIntent.putExtra(ADD, true)
            } else {
                lIntent.putExtra(ADD, false)
            }
            startActivity(lIntent)
        }

    }

    override fun onResume() {
        super.onResume()
        menuGeneration()
    }

    private fun menuGeneration() {
        menu = ArrayList()
        for (data in obj!!.datas) {
            menu.add(data.name)
        }
        menu.add(getString(R.string.add_new_data))
        val adapter = ObjectDataAdapter(this, android.R.layout.simple_list_item_1, menu)




        listView.adapter = adapter
        listView.invalidateViews()
    }

    companion object {

        var OBJECT_POSITION = "object_pos"
        var DATA_POSITION = "data_position"
        var ADD = "add"
    }
}
