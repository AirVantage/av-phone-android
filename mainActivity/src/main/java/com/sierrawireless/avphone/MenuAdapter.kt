package com.sierrawireless.avphone

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.util.*

class MenuAdapter internal constructor(private val activity: Activity, var list: ArrayList<MenuEntry>) : BaseAdapter() {

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


        val name: TextView
        if (convertView == null) {

            lConvertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)

        }
        val entry = list[position]
        if (entry.type == MenuEntryType.TITLE) {
            name = lConvertView!!.findViewById(android.R.id.text1)
            name.text = entry.name
            name.setTypeface(name.typeface, Typeface.BOLD)
            name.setBackgroundColor(lConvertView!!.resources.getColor(R.color.navy))
            name.setTextColor(Color.WHITE)
        } else {
            name = lConvertView!!.findViewById(android.R.id.text1)
            name.text = entry.name
            name.setBackgroundColor(Color.WHITE)
            name.setTextColor(lConvertView!!.resources.getColor(R.color.navy))
        }


        return lConvertView!!
    }
}
