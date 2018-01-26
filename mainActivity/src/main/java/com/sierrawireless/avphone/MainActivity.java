package com.sierrawireless.avphone;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationManager;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.MonitoringService.ServiceBinder;
import com.sierrawireless.avphone.task.AsyncTaskFactory;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvResult;

import net.airvantage.model.AvSystem;
import net.airvantage.model.User;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * The main activity, handling drawer and Fragments
 */
public class MainActivity extends FragmentActivity

        implements LoginListener, AuthenticationManager, OnSharedPreferenceChangeListener,
        MonitorServiceManager, CustomLabelsManager, SyncWithAvListener {

    private static final String TAG = "MainActivity";
    private static final String PREFERENCE_SYSTEM_NAME = "systemName";
    private static final String PREFERENCE_SYSTEM_SERIAL = "systemSerial";
    private static final String PREFERENCE_USER_NAME = "userName";
    private static final String PREFERENCE_USER_UID = "userUid";
    private String objectName;

    private ActionBar actionBar;
    private AlarmManager alarmManager;
    private IAsyncTaskFactory taskFactory;
    private Authentication auth;
    private SharedPreferences prefs;

    boolean boundToMonitoringService = false;
    MonitoringService monitoringService;
    private MonitorServiceListener monitoringServiceListener = null;

    private CustomLabelsListener customLabelsListener = null;
    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private ActionBarDrawerToggle drawerToggle;

    private final static String FRAGMENT_HOME = "Home";
    private final static String FRAGMENT_CONFIGURE = "Objects...";
    private final static String FRAGMENT_SETTINGS = "Settings";
    private final static String FRAGMENT_FAQ = "FAQ";

    private ConfigureFragment configureFragment;
    private HomeFragment homeFragment;
    private ArrayList<RunFragment> runFragment;

    private int lastPosition = 0;

    private Boolean serviceSendData = false;
    ObjectsManager objectsManager;

    @SuppressLint("StaticFieldLeak")
    static MainActivity instance;

    private static String[] FRAGMENT_LIST;

    public String[] buildFragmentList() {
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add(FRAGMENT_HOME);
        for (AvPhoneObject object: objectsManager.objects) {
            Log.d(TAG, "buildFragmentList: add object " + object.name);
            tmp.add(object.name);
        }
        tmp.add(FRAGMENT_CONFIGURE);
        tmp.add(FRAGMENT_SETTINGS);
        tmp.add(FRAGMENT_FAQ);
        String[] array = new String[tmp.size()];
        return tmp.toArray(array);
    }

    public void setCustomLabelsListener(CustomLabelsListener customLabelsListener) {
        this.customLabelsListener = customLabelsListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MainActivity.instance = this;
        // Initialization og Object Manager
        objectsManager = ObjectsManager.getInstance();
        objectsManager.init(this);
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());

        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        readAuthenticationFromPreferences();

        taskFactory = new AsyncTaskFactory(MainActivity.this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        loadMenu();

        drawerListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open,
                R.string.drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);


    }

    public void loadMenu() {
        FRAGMENT_LIST = buildFragmentList();
        drawerListView = (ListView) findViewById(R.id.left_drawer);

        drawerListView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_item, buildFragmentList()));

        drawerListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Set the list's click listener
        drawerListView.invalidateViews();
        final Map<String, Fragment> fragments = initFragments();
        final Fragment currentFragment;
        if (isLogged()) {

            currentFragment = fragments.get(objectsManager.getCurrentObjectName());
            unlockDrawer();
            if (isServiceRunning()) {
                connectToService();
            }

        } else {

            currentFragment = fragments.get(FRAGMENT_HOME);
            lockDrawer();
            if (isServiceRunning()) {
                // The token is probably expired.
                // We stop the service since the "stop" button is not available anymore.
                this.stopMonitoringService();
            }

        }

        selectItem(currentFragment);

    }
    @Override
    protected void onResume() {
        super.onResume();
        drawerListView.requestFocusFromTouch();
        drawerListView.setItemChecked(lastPosition, true);
        drawerListView.setSelection(lastPosition);
        drawerListView.refreshDrawableState();
        //drawerListView.setSelection();

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return // Handle it ourselves
                drawerToggle.onOptionsItemSelected(item)
                        // Or just go with defaults
                        || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void lockDrawer() {

        actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
    }

    private void unlockDrawer() {

        actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            drawerLayout.openDrawer(Gravity.START);
        }

        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(true);
        }
    }

    private void readAuthenticationFromPreferences() {
        this.auth = PreferenceUtils.readAuthentication(this);
    }

    public boolean isLogged() {
        return (this.auth != null && !this.auth.isExpired(new Date()));
    }

    @Override
    public void onAuthentication(Authentication auth) {

        this.auth = auth;
        unlockDrawer();

        saveAuthenticationInPreferences(auth);
     }

    @Override
    public Authentication getAuthentication() {
        return this.auth;
    }

    private void saveAuthenticationInPreferences(Authentication auth) {
        PreferenceUtils.saveAuthentication(this, auth);
    }

    public void forgetAuthentication() {

        lockDrawer();
        PreferenceUtils.resetAuthentication(this);
        this.auth = null;

        if (this.isServiceRunning()) {
            this.stopMonitoringService();
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (PreferenceUtils.PREF_SERVER_KEY.equals(key) || PreferenceUtils.PREF_CLIENT_ID_KEY.equals(key)) {

            this.stopMonitoringService();
            this.forgetAuthentication();

        } else if (PreferenceUtils.PREF_PERIOD_KEY.equals(key) || PreferenceUtils.PREF_PASSWORD_KEY.equals(key)) {

            this.restartMonitoringService();

        } else if (key.contains("pref_data_custom")) {

            if (this.customLabelsListener != null) {
                this.customLabelsListener.onCustomLabelsChanged();
            }
        }

    }

    public boolean isServiceRunning() {
       //return serviceSendData;

        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            Log.e(TAG, "isServiceRunning: can't get activity service");
            Toast.makeText(getApplicationContext(), "can't get activity service" ,Toast.LENGTH_SHORT).show();
            return false;
        }
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return serviceSendData;
            }
        }
        return false;

    }

    public boolean isServiceStarted(String name) {
        if (this.objectName == null) {
            return false;
        }
        if (!this.objectName.equals(name)) {
            return false;
        }
        //return serviceSendData;

        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            Log.e(TAG, "isServiceRunning: can't get activity service");
            Toast.makeText(getApplicationContext(), "can't get activity service" ,Toast.LENGTH_SHORT).show();
            return false;
        }
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    public boolean oneServiceStarted() {
        //return serviceSendData;

        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            Log.e(TAG, "isServiceRunning: can't get activity service");
            Toast.makeText(getApplicationContext(), "can't get activity service" ,Toast.LENGTH_SHORT).show();
            return false;
        }
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    public void connectToService() {
        Intent intent = new Intent(this, MonitoringService.class);
        boundToMonitoringService = this.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void disconnectFromService() {
        if (boundToMonitoringService) {
            this.unbindService(connection);
            boundToMonitoringService = false;
            if (monitoringServiceListener != null) {
                monitoringServiceListener.onServiceStopped(monitoringService);
            }
        }
    }

    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.d(TAG, "Connected to the monitoring service");
            monitoringService = ((ServiceBinder) binder).getService();

            if (monitoringServiceListener != null) {
                monitoringServiceListener.onServiceStarted(monitoringService);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "Disconnected from the monitoring service");
            boundToMonitoringService = false;
        }

    };

    public void sendAlarmEvent() {
        //if (boundToMonitoringService && monitoringService != null) {
            monitoringService.sendAlarmEvent();
       // }
    }

    @Override
    public void startMonitoringService(String name) {
        this.objectName = name;
        AvPhonePrefs avPrefs = PreferenceUtils.getAvPhonePrefs(this);

        Intent intent = new Intent(this, MonitoringService.class);
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this));
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost);
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password);
        intent.putExtra(MonitoringService.CONNECT, false);
        intent.putExtra(MonitoringService.OBJECT_NAME, name);


        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        // registering our pending intent with alarm manager
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, pendingIntent);

         connectToService();
    }

    @Override
    public void startSendData(){
        AvPhonePrefs avPrefs = PreferenceUtils.getAvPhonePrefs(this);

        Intent intent = new Intent(this, MonitoringService.class);
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this));
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost);
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password);
        intent.putExtra(MonitoringService.CONNECT, true);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        // registering our pending intent with alarm manager
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000,
                Integer.valueOf(avPrefs.period) * 60 * 1000, pendingIntent);

       // connectToService();
        serviceSendData = true;

    }

    @Override
    public void stopSendData(){
        Intent intent = new Intent(this, MonitoringService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);
        serviceSendData = false;
    }

    @Override
    public void stopMonitoringService() {
        Intent intent = new Intent(this, MonitoringService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);
        this.stopService(intent);

        disconnectFromService();
    }

    private void restartMonitoringService() {
        stopMonitoringService();
        startMonitoringService(objectName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromService();
    }

    @Override
    public MonitoringService getMonitoringService() {
        return this.monitoringService;
    }

    public void setMonitoringServiceListener(MonitorServiceListener listener) {
        this.monitoringServiceListener = listener;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onSynced(SyncWithAvResult result) {

        final AvSystem system = result.getSystem();
        if (system == null){
            return;
        }
        prefs.edit().putString("systemUid", system.uid).apply();
        prefs.edit().putString(PREFERENCE_SYSTEM_NAME, system.name).apply();

        final User user = result.getUser();
        prefs.edit().putString(PREFERENCE_USER_UID, user.uid).apply();
        prefs.edit().putString(PREFERENCE_USER_NAME, user.name).apply();

        final String deviceSerial = DeviceInfo.generateSerial(user.uid);
        prefs.edit().putString(PREFERENCE_SYSTEM_SERIAL, deviceSerial).apply();

        if (runFragment != null) {
            String systemUid = this.getSystemUid();
            String systemName = this.getSystemName();
            for (RunFragment tmp:runFragment) {
                tmp.setLinkToSystem(systemUid, systemName);
            }
        } else {
            Log.w(TAG, "RunFragment reference is null when onSynced is called");
        }

    }

    public String getSystemUid() {
        return prefs.getString("systemUid", null);
    }

    public String getSystemSerial() {
        return prefs.getString(PREFERENCE_SYSTEM_SERIAL, null);
    }

    public String getSystemName() {
        return prefs.getString(PREFERENCE_SYSTEM_NAME, null);
    }

    @SuppressLint("DefaultLocale")
    public void setSystemSerial(final String serial) {
        prefs.edit().putString(PREFERENCE_SYSTEM_SERIAL, serial.toUpperCase()).apply();
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(final int position) {

        final Fragment fragment = getFragment(position);
        if (fragment == null) {
            //No item check if the position is valid
            Log.d(TAG, "selectItem: Fragment list " + FRAGMENT_LIST[position]);
            if (FRAGMENT_LIST[position].equals(FRAGMENT_FAQ)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://doc.airvantage.net/alms/"));
                startActivity(browserIntent);
                drawerListView.setSelection(lastPosition);
                drawerLayout.closeDrawer(drawerListView);
            }
            return;
        }

        // Insert the fragment by replacing any existing fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        drawerListView.setItemChecked(position, true);
        setTitle(FRAGMENT_LIST[position]);
        drawerListView.setSelection(position);
        drawerLayout.closeDrawer(drawerListView);
        lastPosition = position;
    }

    public void goHomeFragment() {
        final Fragment fragment = getFragment(0);
        // Insert the fragment by replacing any existing fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        drawerListView.setItemChecked(0, true);
        setTitle(FRAGMENT_LIST[0]);
        drawerListView.setSelection(0);
        drawerLayout.closeDrawer(drawerListView);
        lastPosition = 0;

    }
    private void selectItem(final Fragment fragment) {
        final Iterator<Fragment> fragmentsIterator = initFragments().values().iterator();
        for (int position = 0; fragmentsIterator.hasNext(); position++) {
            final Fragment currentFragment = fragmentsIterator.next();
            if (fragment.getId() == currentFragment.getId()) {
                selectItem(position);
                return;
            }
        }
    }


    private Map<String, Fragment> initFragments() {

        if (configureFragment == null) {
            configureFragment = new ConfigureFragment();
            configureFragment.setTaskFactory(taskFactory);
        }

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            homeFragment.setTaskFactory(taskFactory);
        }

        runFragment = new ArrayList<>();

        RunFragment tmp;
        for (AvPhoneObject object: objectsManager.objects) {
            tmp = new RunFragment();
            tmp.setTaskFactory(taskFactory);
            Log.d(TAG, "initFragments: set " + object.name);
            tmp.setObjectName(object.name);
            runFragment.add(tmp);
        }


        final HashMap<String, Fragment> fragmentsMapping = new HashMap<>();
        fragmentsMapping.put(FRAGMENT_CONFIGURE, configureFragment);
        fragmentsMapping.put(FRAGMENT_HOME, homeFragment);
        fragmentsMapping.put(FRAGMENT_SETTINGS, new SettingsActivity.SettingsFragment());
        int pos = 0;
        for (AvPhoneObject object: objectsManager.objects) {
            fragmentsMapping.put(object.name, runFragment.get(pos));
            pos++;
        }

        return fragmentsMapping;
    }

    @Nullable
    private Fragment getFragment(final int fragmentPosition) {
        final String fragmentName = FRAGMENT_LIST[fragmentPosition];
        final Map<String, Fragment> fragmentMap = initFragments();
        return fragmentMap.containsKey(fragmentName) ? fragmentMap.get(fragmentName) : null;
    }

}
