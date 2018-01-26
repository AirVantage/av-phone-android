package com.sierrawireless.avphone.adapter;

import android.app.Activity;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sierrawireless.avphone.R;

import java.util.ArrayList;


/**
 * Created by JDamiano on 26/01/2018.
 */

public class AvPhoneObjectDataAdapter extends BaseAdapter {
    public ArrayList<String> list;
    Activity activity;
    TextView name;
    int resource;
    private static final String TAG = "AvPhoneObjectDataAdapte";

    public AvPhoneObjectDataAdapter(Activity activity, int resource,  ArrayList<String> list){
        super();
        this.activity=activity;
        this.list=list;
        this.resource = resource;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        // menu type count
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        // current menu type
        if (position == list.size()-1) {
            Log.d(TAG, "getItemViewType: position " + position);
            return 1;
        }else{
            return 0;
        }
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub


        LayoutInflater inflater=activity.getLayoutInflater();

        if(convertView == null){

            convertView=inflater.inflate(resource, null);

            name=(TextView) convertView.findViewById(android.R.id.text1);
        }

        name.setText(list.get(position));

        return convertView;
    }

}
