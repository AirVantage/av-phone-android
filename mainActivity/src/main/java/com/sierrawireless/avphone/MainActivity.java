package com.sierrawireless.avphone;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
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
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationManager;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * The main activity, in charge of displaying the tab view
 */
public class MainActivity extends FragmentActivity
        implements /* TabListener, */ LoginListener, AuthenticationManager,
        OnSharedPreferenceChangeListener, MonitorServiceManager, CustomLabelsManager, SyncWithAvListener {

    private static final String PREFERENCE_SYSTEM_NAME = "systemName";
    private static final String PREFERENCE_SYSTEM_SERIAL = "systemSerial";
    private static final String PREFERENCE_USER_NAME = "userName";
    private static final String PREFERENCE_USER_UID = "userUid";

    private static String LOGTAG = MainActivity.class.getName();

    //   private ViewPager viewPager;
    //    private TabsPagerAdapter tabsPageAdapter;
    private ActionBar actionBar;
    private AlarmManager alarmManager;
    private IAsyncTaskFactory taskFactory;
    private Authentication auth;
    private SharedPreferences prefs;

    boolean boundToMonitoringService = false;
    MonitoringService monitoringService;
    private MonitorServiceListener monitoringServiceListener = null;

    private CustomLabelsListener customLabelsListener = null;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private final static String FRAGMENT_HOME = "Home";
    private final static String FRAGMENT_RUN = "Run";
    private final static String FRAGMENT_CONFIGURE = "Configure";
    private final static String FRAGMENT_SETTINGS = "Settings";

    private ConfigureFragment configureFragment;
    private HomeFragment homeFragment;
    private RunFragment runFragment;

    private final static String[] FRAGMENT_LIST = new String[]{
            FRAGMENT_HOME,
            FRAGMENT_RUN,
            FRAGMENT_CONFIGURE,
            FRAGMENT_SETTINGS,
    };

    public void setCustomLabelsListener(CustomLabelsListener customLabelsListener) {
        this.customLabelsListener = customLabelsListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        readAuthenticationFromPreferences();

        taskFactory = new AsyncTaskFactory(MainActivity.this);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_item, FRAGMENT_LIST));

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
         /*      R.drawable.ic_launcher, */R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final Map<String, Fragment> fragments = initFragments();
        final Fragment currentFragment;
        if (isLogged()) {

            currentFragment = fragments.get(FRAGMENT_RUN);
            if (isServiceRunning()) {
                connectToService();
            }

        } else {

            currentFragment = fragments.get(FRAGMENT_HOME);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            if (isServiceRunning()) {
                // The token is probably expired.
                // We stop the service since the "stop" button is not available anymore.
                this.stopMonitoringService();
            }

        }

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, currentFragment)
                .commit();

    }

    private void readAuthenticationFromPreferences() {
        this.auth = PreferenceUtils.readAuthentication(this);
    }

    @Override
    public void OnLoginChanged(boolean logged) {
    }

    public boolean isLogged() {
        return (this.auth != null && !this.auth.isExpired(new Date()));
    }

    @Override
    public void onAuthentication(Authentication auth) {

        this.auth = auth;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

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

        PreferenceUtils.resetAuthentication(this);

        this.auth = null;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

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

        } else if (key.indexOf("pref_data_custom") != -1) {

            if (this.customLabelsListener != null) {
                this.customLabelsListener.onCustomLabelsChanged();
            }
        }

    }

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void connectToService() {
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
            Log.d(LOGTAG, "Connected to the monitoring service");
            monitoringService = ((ServiceBinder) binder).getService();

            if (monitoringServiceListener != null) {
                monitoringServiceListener.onServiceStarted(monitoringService);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(LOGTAG, "Disconnected from the monitoring service");
            boundToMonitoringService = false;
        }

    };

    public void sendAlarmEvent(boolean activated) {
        if (boundToMonitoringService && monitoringService != null) {
            monitoringService.sendAlarmEvent(activated);
        }
    }

    @Override
    public void startMonitoringService() {
        AvPhonePrefs avPrefs = PreferenceUtils.getAvPhonePrefs(this);

        Intent intent = new Intent(this, MonitoringService.class);
        intent.putExtra(MonitoringService.DEVICE_ID, DeviceInfo.getUniqueId(this));
        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost);
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // registering our pending intent with alarm manager
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, Integer.valueOf(avPrefs.period) * 60 * 1000,
                pendingIntent);

        connectToService();
    }

    @Override
    public void stopMonitoringService() {
        Intent intent = new Intent(this, MonitoringService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);
        this.stopService(intent);

        disconnectFromService();
    }

    private void restartMonitoringService() {
        stopMonitoringService();
        startMonitoringService();
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
        prefs.edit().putString("systemUid", system.uid).commit();
        prefs.edit().putString(PREFERENCE_SYSTEM_NAME, system.name).commit();

        final User user = result.getUser();
        prefs.edit().putString(PREFERENCE_USER_UID, user.uid).commit();
        prefs.edit().putString(PREFERENCE_USER_NAME, user.name).commit();

        final String deviceSerial = DeviceInfo.generateSerial(user.uid, system.type);
        prefs.edit().putString(PREFERENCE_SYSTEM_SERIAL, deviceSerial).commit();

        if (runFragment != null) {
            String systemUid = this.getSystemUid();
            String systemName = this.getSystemName();
            runFragment.setLinkToSystem(systemUid, systemName);
        } else {
            Log.w(LOGTAG, "RunFragment reference is null when onSynced is called");
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
        prefs.edit().putString(PREFERENCE_SYSTEM_SERIAL, serial.toUpperCase()).commit();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(final int position) {

        final Fragment fragment = getFragment(position);
        if (fragment == null) {
            return;
        }

        // Insert the fragment by replacing any existing fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(FRAGMENT_LIST[position]);
        mDrawerList.setSelection(position);
        mDrawerLayout.closeDrawer(mDrawerList);
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

        if (runFragment == null) {
            runFragment = new RunFragment();
        }

        final HashMap<String, Fragment> fragmentsMapping = new HashMap<>();
        fragmentsMapping.put(FRAGMENT_CONFIGURE, configureFragment);
        fragmentsMapping.put(FRAGMENT_HOME, homeFragment);
        fragmentsMapping.put(FRAGMENT_SETTINGS, new SettingsActivity.SettingsFragment());
        fragmentsMapping.put(FRAGMENT_RUN, runFragment);

        return fragmentsMapping;
    }

    @Nullable
    private Fragment getFragment(final int fragmentPosition) {
        final String fragmentName = FRAGMENT_LIST[fragmentPosition];
        final Map<String, Fragment> fragmentMap = initFragments();
        return fragmentMap.containsKey(fragmentName) ? fragmentMap.get(fragmentName) : null;
    }

}