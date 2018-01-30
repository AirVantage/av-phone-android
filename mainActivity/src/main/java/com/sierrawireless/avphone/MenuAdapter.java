package com.sierrawireless.avphone;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MenuAdapter extends BaseAdapter {
    public ArrayList<MenuEntry> list;
    private Activity activity;
    private TextView name;

    public MenuAdapter(Activity activity, ArrayList<MenuEntry> list){
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

            convertView=inflater.inflate(android.R.layout.simple_list_item_1, parent, false);

            name = convertView.findViewById(R.id.name);
        }
        MenuEntry entry =list.get(position);
        if (entry.type == MenuEntryType.TITLE){
            name= convertView.findViewById(android.R.id.text1);
            name.setText(entry.name);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setBackgroundColor(convertView.getResources().getColor(R.color.navy));
            name.setTextColor(Color.WHITE);
        }else{
            name= convertView.findViewById(android.R.id.text1);
            name.setText(entry.name);
            name.setBackgroundColor(Color.WHITE);
            name.setTextColor(convertView.getResources().getColor(R.color.navy));
        }


        return convertView;
    }
}
