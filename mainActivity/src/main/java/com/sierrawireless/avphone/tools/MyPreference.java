package com.sierrawireless.avphone.tools;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.sierrawireless.avphone.model.AvPhoneObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MyPreference {
    private SharedPreferences preferences;

    public MyPreference(SharedPreferences prefences) {
        this.preferences = prefences;
    }

    /**
     * Get String value from SharedPreferences at 'key'. If key not found, return ""
     * @param key SharedPreferences key
     * @return String value at 'key' or "" (empty String) if key not found
     */
    public String getString(String key) {
        return preferences.getString(key, "");
    }

    /**
     * Get parsed ArrayList of String from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * @return ArrayList of String
     */
    private ArrayList<String> getListString(String key) {
        return new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }

    public ArrayList<AvPhoneObject> getListObject(String key, Class<AvPhoneObject> mClass){
        Gson gson = new Gson();

        ArrayList<String> objStrings = getListString(key);
        ArrayList<AvPhoneObject> objects = new ArrayList<>();

        for(String jObjString : objStrings){
            AvPhoneObject value  = gson.fromJson(jObjString,  mClass);
            objects.add(value);
        }
        return objects;
    }


    /**
     * Get int value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * @return int value at 'key' or 'defaultValue' if key not found
     */
    public int getInt(String key) {
        return preferences.getInt(key, 0);
    }

    /**
     * Put int value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param value int value to be added
     */
    public void putInt(String key, int value) {
        checkForNullKey(key);
        preferences.edit().putInt(key, value).apply();
    }
    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     * @param key pref key
     */
    private void checkForNullKey(String key){
        if (key == null){
            throw new NullPointerException();
        }
    }

    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param stringList ArrayList of String to be added
     */
    private void putListString(String key, ArrayList<String> stringList) {
        checkForNullKey(key);
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }

    public void putListObject(String key, ArrayList<AvPhoneObject> objArray){
        checkForNullKey(key);
        Gson gson = new Gson();
        ArrayList<String> objStrings = new ArrayList<>();
        for(Object obj : objArray){
            objStrings.add(gson.toJson(obj));
        }
        putListString(key, objStrings);
    }
}
