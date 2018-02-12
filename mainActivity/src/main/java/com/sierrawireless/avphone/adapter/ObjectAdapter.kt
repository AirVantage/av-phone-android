package com.sierrawireless.avphone.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.sierrawireless.avphone.ConfigureFragment
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import java.util.*


class ObjectAdapter(private val activity: Activity, private val resource: Int, var list: ArrayList<String>) : BaseAdapter() {
    private var name: TextView? = null

    private val objectsManager: ObjectsManager = ObjectsManager.getInstance()

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var lConvertView = convertView


        val inflater = activity.layoutInflater

        if (convertView == null) {

            lConvertView = inflater.inflate(resource, null)

            name = lConvertView!!.findViewById(R.id.text)
        }
        val deleteBtn:ImageButton = lConvertView!!.findViewById(R.id.menuDeleteBtn)
        val deleteActionBtn: Button = lConvertView.findViewById(R.id.menuDeleteActionBtn)
        deleteActionBtn.tag = position
        deleteBtn.setOnClickListener {
            deleteBtn.visibility = View.GONE
            deleteActionBtn.visibility = View.VISIBLE
            deleteActionBtn.setOnClickListener{
                val objectsManager = ObjectsManager.getInstance()
                val lPosition = it.tag as Int
                //delete
                objectsManager.setSavedPosition(lPosition)
                ConfigureFragment.instance!!.delete()
                ConfigureFragment.instance!!.delete = true

            }
        }

        if (position == objectsManager.current) {
            deleteBtn.visibility = View.GONE
        } else {
            deleteBtn.visibility = View.VISIBLE
        }
        name!!.text = list[position]

        return lConvertView
    }

}
