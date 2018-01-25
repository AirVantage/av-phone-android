package com.sierrawireless.avphone.tools;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.sierrawireless.avphone.model.AvPhoneObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MyPreference {
    SharedPreferences preferences;

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
    public ArrayList<String> getListString(String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }

    public ArrayList<AvPhoneObject> getListObject(String key, Class<AvPhoneObject> mClass){
        Gson gson = new Gson();

        ArrayList<String> objStrings = getListString(key);
        ArrayList<AvPhoneObject> objects =  new ArrayList<AvPhoneObject>();

        for(String jObjString : objStrings){
            AvPhoneObject value  = gson.fromJson(jObjString,  mClass);
            objects.add(value);
        }
        return objects;
    }



    public <T> T getObject(String key, Class<T> classOfT){

        String json = getString(key);
        Object value = new Gson().fromJson(json, classOfT);
        if (value == null)
            throw new NullPointerException();
        return (T)value;
    }

    /**
     * Get int value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * @param defaultValue int value returned if key was not found
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
     * @param the pref key
     */
    public void checkForNullKey(String key){
        if (key == null){
            throw new NullPointerException();
        }
    }
    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     * @param the pref key
     */
    public void checkForNullValue(String value){
        if (value == null){
            throw new NullPointerException();
        }
    }

    /**
     * Put String value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param value String value to be added
     */
    public void putString(String key, String value) {
        checkForNullKey(key); checkForNullValue(value);
        preferences.edit().putString(key, value).apply();
    }

    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param stringList ArrayList of String to be added
     */
    public void putListString(String key, ArrayList<String> stringList) {
        checkForNullKey(key);
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }
    /**
     * Put ObJect any type into SharedPrefrences with 'key' and save
     * @param key SharedPreferences key
     * @param obj is the Object you want to put
     */
    public void putObject(String key, Object obj){
        checkForNullKey(key);
        Gson gson = new Gson();
        putString(key, gson.toJson(obj));
    }

    public void putListObject(String key, ArrayList<AvPhoneObject> objArray){
        checkForNullKey(key);
        Gson gson = new Gson();
        ArrayList<String> objStrings = new ArrayList<String>();
        for(Object obj : objArray){
            objStrings.add(gson.toJson(obj));
        }
        putListString(key, objStrings);
    }
}
