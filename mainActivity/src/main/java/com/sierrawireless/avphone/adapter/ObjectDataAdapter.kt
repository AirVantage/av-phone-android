package com.sierrawireless.avphone.adapter

import android.app.Activity
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.activity.ObjectConfigureActivity
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
        // The last item must not be deleted
        return if (position == list.size - 1) {
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

            name = lConvertView!!.findViewById(R.id.text)
        }

        val deleteBtn: ImageButton = lConvertView!!.findViewById(R.id.menuDeleteBtn)

        if (position == list.size - 1) {
            deleteBtn.visibility = View.GONE
            name!!.setCompoundDrawablesWithIntrinsicBounds(null,  null, ContextCompat.getDrawable(lConvertView.context, R.drawable.ic_add_data),null)
        }else {
            @Suppress("NAME_SHADOWING")
            val deleteBtn: ImageButton = lConvertView.findViewById(R.id.menuDeleteBtn)
            val deleteActionBtn: Button = lConvertView.findViewById(R.id.menuDeleteActionBtn)
            deleteActionBtn.tag = position
            deleteBtn.setOnClickListener {
                deleteBtn.visibility = View.GONE
                deleteActionBtn.visibility = View.VISIBLE
                deleteActionBtn.setOnClickListener {
                    ObjectConfigureActivity.instance!!.obj!!.datas.removeAt(position)
                    ObjectConfigureActivity.instance!!.menuGeneration()
                }
            }
        }

        name!!.text = list[position]

        return lConvertView
    }
}
