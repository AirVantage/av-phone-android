package com.sierrawireless.avphone.adapter

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.activity.MainActivity
import java.util.*


class MenuAdapter internal constructor(private val activity: Activity, var list: ArrayList<MenuEntry>) : BaseAdapter() {
    val objectsManager:ObjectsManager? = ObjectsManager.getInstance()

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
        lConvertView = convertView ?: inflater.inflate(R.layout.menu_layout, parent, false)
        val entry = list[position]
        if (entry.type == MenuEntryType.TITLE) {
            name = lConvertView!!.findViewById(R.id.text1)
            name.text = entry.name
            name.setTypeface(name.typeface, Typeface.BOLD)


            name.setTextColor(Color.WHITE)
          //  name.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(lConvertView.context, android.R.drawable.ic_lock_silent_mode), null, null, null)

        } else {
            name = lConvertView!!.findViewById(R.id.text1)
            name.text = entry.name
            if (MainActivity.instance.lastPosition != 0 && position != list.size - 1 && position == MainActivity.instance.lastPosition) {
                name.setBackgroundColor(Color.LTGRAY)
            }else {
                if (entry.type == MenuEntryType.COMMAND) {
                    name.setBackgroundColor(Color.WHITE)
                }
            }
            name.setTextColor(ContextCompat.getColor(lConvertView.context, R.color.navy))
            if (entry.type == MenuEntryType.USER) {
                name.setTextColor(Color.LTGRAY)
                name.setTypeface(null, Typeface.ITALIC)
            }
        }
        if (entry.drawable != null) {
            name.setCompoundDrawablesWithIntrinsicBounds(entry.drawable, null, null, null)
        }
        val menuButton:Button = lConvertView.findViewById(R.id.menu_button)

        menuButton.visibility = if (entry.button) {
                                    View.VISIBLE
                                }else{
                                    View.GONE
                                }
        if (entry.button) {
            menuButton.setOnClickListener {
                MainActivity.instance.goConfigureFragment()
            }
        }

        return lConvertView
    }

}
