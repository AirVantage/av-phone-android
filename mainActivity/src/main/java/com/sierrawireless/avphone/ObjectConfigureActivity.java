package com.sierrawireless.avphone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.sierrawireless.avphone.adapter.ObjectDataAdapter;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;
import com.sierrawireless.avphone.tools.Tools;

import java.util.ArrayList;

public class ObjectConfigureActivity extends Activity {

    Button cancel;
    Button save;
    SwipeMenuListView listView;
    ObjectsManager objectsManager;
    ArrayList<String> menu;
    int position;
    AvPhoneObject object;
    AvPhoneObject tmpObject;
    TextView title;
    TextView name;
    EditText nameEdit;

    public static String OBJECT_POSITION = "object_pos";
    public static String DATA_POSITION = "data_position";
    public static String ADD = "add";
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_configure);
        cancel = (Button)findViewById(R.id.cancel);
        save = (Button)findViewById(R.id.save);
        listView = (SwipeMenuListView)findViewById(R.id.listView);
        objectsManager = ObjectsManager.getInstance();
        title = (TextView)findViewById(R.id.titleObject);
        name = (TextView)findViewById(R.id.nameObject);
        nameEdit = (EditText) findViewById(R.id.objectNameEdit);

        context = this;
        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                switch (menu.getViewType()) {
                    case 0:
                        // create "delete" item
                        SwipeMenuItem deleteItem = new SwipeMenuItem(
                                getApplicationContext());
                        // set item background
                        deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                                0x3F, 0x25)));
                        // set item width
                        deleteItem.setWidth((int) Tools.dp2px(90, context));
                        // set a icon
                        deleteItem.setIcon(android.R.drawable.ic_menu_delete);
                        // add to menu
                        menu.addMenuItem(deleteItem);
                        break;
                }
            }
        };

        listView.setMenuCreator(creator);


        Intent intent = getIntent();
        position = intent.getIntExtra(ConfigureFragment.INDEX, -1);

        if (position == -1) {
            object = new AvPhoneObject();
            title.setText("Add New Object");
            objectsManager.objects.add(object);
            position = objectsManager.objects.size() -1;
        }else {
            object = objectsManager.getObjectByIndex(position);
            name.setVisibility(View.GONE);
            nameEdit.setVisibility(View.GONE);
            title.setText(object.name);
        }
        menu = new ArrayList<>();
        for (AvPhoneObjectData data:object.datas) {
            menu.add(data.name);
        }
        menu.add("Add new data....");
        ObjectDataAdapter adapter = new ObjectDataAdapter(this, android.R.layout.simple_list_item_1, menu);



        listView.setAdapter(adapter);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //reload object from list
                objectsManager.reload();
                Intent i = new Intent();
                setResult(Activity.RESULT_CANCELED, i);
                finish();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nameEdit.getVisibility() != View.GONE) {
                    object.name = nameEdit.getText().toString();
                }
                objectsManager.save();
                Intent i = new Intent();
                i.putExtra(ConfigureFragment.POS, position);
                setResult(Activity.RESULT_OK, i);
                finish();
            }
        });


        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu lmenu, int index) {
                switch (index) {
                    case 0:
                        //delete
                        object.datas.remove(position);
                        menu = new ArrayList<>();
                        for (AvPhoneObjectData data:object.datas) {
                            menu.add(data.name);
                        }
                        menu.add("Add new data....");
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(listView.getContext(), android.R.layout.simple_list_item_1, menu);



                        listView.setAdapter(adapter);
                        listView.invalidateViews();
                        break;

                }
                return false;
            }
        });
        listView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(view.getContext(), ObjectDataActivity.class);
                    intent.putExtra(OBJECT_POSITION, position);
                    intent.putExtra(DATA_POSITION, i);
                    if (i == menu.size()-1) {
                        intent.putExtra(ADD, true);
                    }else{
                        intent.putExtra(ADD, false);
                    }
                    startActivity(intent);
                }
            }
        );

    }

    @Override
    protected void onResume() {
        super.onResume();
        menu = new ArrayList<>();
        for (AvPhoneObjectData data:object.datas) {
            menu.add(data.name);
        }
        menu.add("Add new data....");
        ObjectDataAdapter adapter = new ObjectDataAdapter(this, android.R.layout.simple_list_item_1, menu);




        listView.setAdapter(adapter);
        listView.invalidateViews();
    }
}
