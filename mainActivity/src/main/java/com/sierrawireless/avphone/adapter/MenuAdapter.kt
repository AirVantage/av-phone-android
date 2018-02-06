package com.sierrawireless.avphone.adapter

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.sierrawireless.avphone.R
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
        val lConvertView: View?


        val inflater = activity.layoutInflater


        val name: TextView
        lConvertView = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val entry = list[position]
        if (entry.type == MenuEntryType.TITLE) {
            name = lConvertView!!.findViewById(android.R.id.text1)
            name.text = entry.name
            name.setTypeface(name.typeface, Typeface.BOLD)
            name.setBackgroundColor(ContextCompat.getColor(lConvertView.context, R.color.navy))
            name.setTextColor(Color.WHITE)
          //  name.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(lConvertView.context, android.R.drawable.ic_lock_silent_mode), null, null, null)

        } else {
            name = lConvertView!!.findViewById(android.R.id.text1)
            name.text = entry.name
            name.setBackgroundColor(Color.WHITE)
            name.setTextColor(ContextCompat.getColor(lConvertView.context, R.color.navy))
        }
        if (entry.drawable != null) {
            name.setCompoundDrawablesWithIntrinsicBounds(entry.drawable, null, null, null)
        }

        return lConvertView
    }
}
