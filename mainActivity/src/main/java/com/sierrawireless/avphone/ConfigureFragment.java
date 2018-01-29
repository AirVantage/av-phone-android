package com.sierrawireless.avphone;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.sierrawireless.avphone.adapter.ObjectAdapter;
import com.sierrawireless.avphone.auth.AuthUtils;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.task.DeleteSystemListener;
import com.sierrawireless.avphone.task.DeleteSystemResult;
import com.sierrawireless.avphone.task.DeleteSystemTask;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvResult;
import com.sierrawireless.avphone.task.SyncWithAvTask;
import com.sierrawireless.avphone.tools.Tools;

import java.util.ArrayList;

public class ConfigureFragment extends AvPhoneFragment {
    private static final String TAG = "ConfigureFragment";

    private ObjectsManager objectsManager;
    ArrayList<String> menu;
    public static String INDEX = "index";
    public static int CONFIGURE = 0;
    public static String POS = "position";
    private boolean delete;

    private View view;


    private IAsyncTaskFactory taskFactory;

    public ConfigureFragment() {
        super();
    }

    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        view = inflater.inflate(R.layout.fragment_configure, container, false);

        objectsManager = ObjectsManager.getInstance();


        menu = new ArrayList<>();

        for (AvPhoneObject object: objectsManager.objects) {
            menu.add(object.name);
        }

        SwipeMenuListView listView = view.findViewById(R.id.objectConfigure);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                switch (menu.getViewType()) {
                    case 0:
                        // create "delete" item
                        SwipeMenuItem deleteItem = new SwipeMenuItem(
                                getActivity().getBaseContext());
                        // set item background
                        deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                                0x3F, 0x25)));
                        // set item width
                        deleteItem.setWidth((int) Tools.dp2px(
                                getActivity().getBaseContext()));
                        // set a icon
                        deleteItem.setIcon(android.R.drawable.ic_menu_delete);
                        // add to menu
                        menu.addMenuItem(deleteItem);
                        break;
                }
            }


        };


        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu lmenu, int index) {
                switch (index) {
                    case 0:
                        //delete
                        objectsManager.setSavedPosition(position);
                        delete();
                        delete = true;
                        break;

                }
                return false;
            }
        });

        listView.setMenuCreator(creator);

        ObjectAdapter adapter = new ObjectAdapter(getActivity(), android.R.layout.simple_list_item_1, menu);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Open a new intent with the selected Object
                Log.d(TAG, "onItemClick: " + i + " " +menu.get(i));
                Intent intent = new Intent(view.getContext(), ObjectConfigureActivity.class);
                intent.putExtra(INDEX, i);

                startActivityForResult(intent, CONFIGURE);

            }
        });


        Button doneBtn = view.findViewById(R.id.doneConfigureBtn);
        Button addBtn = view.findViewById(R.id.addConfigureBtn);

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)getActivity()).goHomeFragment();
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ObjectConfigureActivity.class);
                intent.putExtra(INDEX, -1);

                startActivityForResult(intent, CONFIGURE);
            }
        });


        return view;
    }




    private boolean checkCredentials() {

        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        if (!prefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(getActivity());
            return false;
        }

        return true;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AuthorizationActivity.REQUEST_AUTHORIZATION) {
            Authentication auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data);
            if (auth != null) {
                authManager.onAuthentication(auth);
                if (delete) {
                    deleteSysten(auth.getAccessToken());
                }else {
                    syncWithAv(auth.getAccessToken());
                }
            }
        }else if (requestCode == CONFIGURE) {
            if (resultCode == Activity.RESULT_OK) {
                this.delete = false;
                int position = data.getIntExtra(POS, -1);
                //set current and start synchronization

                objectsManager.setSavedPosition(position);

                if (checkCredentials()) {
                    Authentication auth = authManager.getAuthentication();
                    if (auth != null && !auth.isExpired()) {
                        syncWithAv(auth.getAccessToken());
                    } else {
                        this.delete = false;
                        requestAuthentication();
                    }
                }
            }
            MainActivity.instance.loadMenu();


        }
    }

    protected void delete() {
        if (checkCredentials()) {
            Authentication auth = authManager.getAuthentication();
            if (auth != null && !auth.isExpired()) {
                deleteSysten(auth.getAccessToken());
            } else {
                this.delete = true;
                requestAuthentication();
            }
        }
    }

    private void deleteSysten(String token) {
        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        final IMessageDisplayer display = this;

        final DeleteSystemTask deleteTask = taskFactory.deleteSystemTak(prefs.serverHost, token);
        deleteTask.execute();

        deleteTask.addProgressListener(new DeleteSystemListener() {

            public void onDeleting(DeleteSystemResult result) {
                Log.d(TAG, "onSynced: ICI");
                if (delete) {
                    objectsManager.removeSavedObject();
                }

                deleteTask.showResult(result, display, getActivity());
                MainActivity.instance.loadMenu();


            }
        });

    }

    private void syncWithAv(String token) {

        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        final IMessageDisplayer display = this;

        final SyncWithAvTask syncTask = taskFactory.syncAvTask(prefs.serverHost, token);

        SyncWithAvParams syncParams = new SyncWithAvParams();
        syncParams.deviceId = DeviceInfo.getUniqueId(getActivity());
        syncParams.imei = DeviceInfo.getIMEI(getActivity());
        syncParams.deviceName = DeviceInfo.getDeviceName();
        syncParams.iccid = DeviceInfo.getICCID(getActivity());
        syncParams.mqttPassword = prefs.password;
        syncParams.customData = PreferenceUtils.getCustomDataLabels(getActivity());

        syncTask.execute(syncParams);

        syncTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(SyncWithAvResult result) {
                Log.d(TAG, "onSynced: ICI");
                if (delete) {
                    objectsManager.removeSavedObject();
                }

                syncTask.showResult(result, display, getActivity());
                MainActivity.instance.loadMenu();

                if (!result.isError()) {
                    syncListener.onSynced(result);
                }

            }
        });

    }


    public TextView getErrorMessageView() {
       return (TextView) view.findViewById(R.id.configure_error_message);
   }

}
