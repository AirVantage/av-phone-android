package com.sierrawireless.avphone.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.tools.Tools;

import java.util.ArrayList;
import java.util.HashMap;

public class RunListViewAdapter extends BaseAdapter{
    public ArrayList<HashMap<String, String>> list;
    private Activity activity;
    private TextView name;
    private TextView value;

    public RunListViewAdapter(Activity activity, ArrayList<HashMap<String, String>> list){
        super();
        this.activity=activity;
        this.list=list;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub



        LayoutInflater inflater=activity.getLayoutInflater();

        if(convertView == null){

            convertView=inflater.inflate(R.layout.run_column_row, parent, false);

            name = (TextView) convertView.findViewById(R.id.name);
            value = (TextView) convertView.findViewById(R.id.value);
        }

        HashMap<String, String> map=list.get(position);
        name.setText(map.get(Tools.NAME));
        value.setText(map.get(Tools.VALUE));

        return convertView;
    }
}
