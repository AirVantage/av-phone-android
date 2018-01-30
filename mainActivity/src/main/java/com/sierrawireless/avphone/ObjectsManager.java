package com.sierrawireless.avphone;


import android.content.Context;
import android.util.Log;

import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;
import com.sierrawireless.avphone.tools.MyPreference;

import java.util.ArrayList;

public class ObjectsManager {
    private static final String TAG = "ObjectsManager";
    private static ObjectsManager instance = null;
    private MainActivity mainActivyty = null;
    private static String SHARED_PREFS_FILE = "SavedModels";
    private static String MODELS = "models";
    private static String ACTIVE = "active";
    public int current;
    private int savedPosition = -1;

    ArrayList<AvPhoneObject> objects;


    public static ObjectsManager getInstance(){
        if (instance == null) {
            instance = new ObjectsManager();
        }
        return instance;
    }

    private ObjectsManager(){
        objects = new ArrayList<>();
    }

    void init(MainActivity activity) {
        this.mainActivyty = activity;
        MyPreference pref = new MyPreference(activity.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE));


        objects = pref.getListObject(MODELS, AvPhoneObject.class);
        current = pref.getInt(ACTIVE);

        if (objects.isEmpty()) {
            // Create the default model here
            AvPhoneObject model = new AvPhoneObject();
            model.name = "Printer";
            AvPhoneObjectData data = new AvPhoneObjectData("A6 Page Count", "page(s)", "0", AvPhoneObjectData.Mode.UP, "1");
            model.add(data);
            data = new AvPhoneObjectData("A4 Page Count", "page(s)", "0", AvPhoneObjectData.Mode.UP, "2");
            model.add(data);
            data = new AvPhoneObjectData("Black Cartridge S/N", "", "NTOQN-7HUL9-NEPFL-13IOA", AvPhoneObjectData.Mode.None, "3");
            model.add(data);
            data = new AvPhoneObjectData("Black lnk Level", "%", "100", AvPhoneObjectData.Mode.DOWN, "4");
            model.add(data);
            data = new AvPhoneObjectData("Color Cartridge S/N", "", "629U7-XLT5H-6SCGJ-@CENZ", AvPhoneObjectData.Mode.None, "5");
            model.add(data);
            data = new AvPhoneObjectData("Color lnk Level", "%", "100", AvPhoneObjectData.Mode.DOWN, "6");
            model.add(data);
            objects.add(model);
            current = 0;
            saveOnPref();
        }
        if (savedPosition == -1) {
            savedPosition = current;
        }
    }

    void removeSavedObject() {
        objects.remove(savedPosition);
        savedPosition = current;
        saveOnPref();
    }

    private void saveOnPref() {
        MyPreference pref = new MyPreference(mainActivyty.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE));
        // Save the list for later
        pref.putListObject(MODELS, objects);
        pref.putInt(ACTIVE, current);
    }

    void reload() {

        MyPreference pref = new MyPreference(mainActivyty.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE));


        objects = pref.getListObject(MODELS, AvPhoneObject.class);
        current = pref.getInt(ACTIVE);

    }

    public void execOnCurrent() {
        AvPhoneObject object;
        object = objects.get(current);
        object.exec();
        // Save the list for later
        saveOnPref();
    }

    void changeCurrent(String name) {
        Log.d(TAG, "changeCurrent: change axctive to " + name + " current before " + current);
        Integer indice = 0;
        for (AvPhoneObject object: objects) {
            if (object.name.equals(name)) {
                current = indice;
                Log.d(TAG, "changeCurrent: current after " + current);
                saveOnPref();
                return;
            }
            indice++;
        }
    }

    void  setSavedPosition(int position) {
        savedPosition = position;
    }

    public AvPhoneObject getSavecObject() {
        return objects.get(savedPosition);
    }

    public String getSavedObjectName() {
        return objects.get(savedPosition).name;
    }

    AvPhoneObject getObjectByIndex(int position) {
        if (position > objects.size()) {
            return null;
        }
        return objects.get(position);
    }

    public AvPhoneObject getCurrentObject() {
        if (objects.isEmpty()) {
            return null;
        }
        return objects.get(current);
    }

    AvPhoneObject getObjectByName(String name) {
        for (AvPhoneObject object: objects) {
            if (object.name.equals(name)) {
                return object;
            }
        }
        return null;
    }

    void save() {
        saveOnPref();

    }




}
