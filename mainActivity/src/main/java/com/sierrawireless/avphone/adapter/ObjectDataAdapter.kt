package com.sierrawireless.avphone.adapter

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.util.*


class ObjectDataAdapter(private val activity: Activity, private val resource: Int, var list: ArrayList<String>) : BaseAdapter() {
    private var name: TextView? = null

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getViewTypeCount(): Int {
        // menu type count
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        // current menu type
        return if (position == list.size - 1) {
            Log.d(TAG, "getItemViewType: position " + position)
            1
        } else {
            0
        }
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var lConvertView = convertView


        val inflater = activity.layoutInflater

        if (convertView == null) {

            lConvertView = inflater.inflate(resource, null)

            name = lConvertView!!.findViewById(android.R.id.text1)
        }

        name!!.text = list[position]

        return lConvertView!!
    }

    companion object {
        private const val TAG = "ObjectDataAdapter"
    }

}
