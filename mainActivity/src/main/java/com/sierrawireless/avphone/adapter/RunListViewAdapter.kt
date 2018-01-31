package com.sierrawireless.avphone.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.tools.Tools
import java.util.*

class RunListViewAdapter(private val activity: Activity, var list: ArrayList<HashMap<String, String>>) : BaseAdapter() {
    private var name: TextView? = null
    private var value: TextView? = null

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

            lConvertView = inflater.inflate(R.layout.run_column_row, parent, false)

            name = lConvertView!!.findViewById(R.id.name)
            value = lConvertView.findViewById(R.id.value)
        }

        val map = list[position]
        name!!.text = map[Tools.NAME]
        value!!.text = map[Tools.VALUE]

        return lConvertView!!
    }
}
