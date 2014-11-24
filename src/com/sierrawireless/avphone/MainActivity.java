package com.sierrawireless.avphone;

import java.util.Date;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.FragmentTransaction;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationListener;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.MonitoringService.ServiceBinder;
import com.sierrawireless.avphone.task.AsyncTaskFactory;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;

/**
 * The main activity, in charge of displaying the tab view
 */
public class MainActivity extends FragmentActivity implements TabListener, LoginListener, AuthenticationListener,
        OnSharedPreferenceChangeListener, MonitorServiceManager, CustomLabelsManager {

    private static String LOGTAG = MainActivity.class.getName();

    private ViewPager viewPager;
    private TabsPagerAdapter tabsPageAdapter;
    private ActionBar actionBar;
    private AlarmManager alarmManager;
    private IAsyncTaskFactory taskFactory;
    private Authentication auth;
    private SharedPreferences prefs;

    boolean boundToMonitoringService = false;
    MonitoringService monitoringService;
    private MonitorServiceListener monitoringServiceListener = null;

    private CustomLabelsListener customLabelsListener = null;

    public void setCustomLabelsListener(CustomLabelsListener customLabelsListener) {
        this.customLabelsListener = customLabelsListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        readAuthenticationFromPreferences();

        taskFactory = new AsyncTaskFactory(MainActivity.this);

        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        tabsPageAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(tabsPageAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        // Adding Tabs
        actionBar.addTab(actionBar.newTab().setText(getString(R.string.home_tab)).setTabListener(this));

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        if (isLogged()) {
            showLoggedTabs();
        } else {
            hideLoggedTabs();
        }

        if (isServiceRunning()) {
            connectToService();
        }

    }

    private void readAuthenticationFromPreferences() {

        String accessToken = prefs.getString(PreferenceUtils.PREF_ACCESS_TOKEN, null);

        Long expiresAtMs = null;

        try {
            expiresAtMs = prefs.getLong(PreferenceUtils.PREF_TOKEN_EXPIRES_AT, 0);
        } catch (ClassCastException e) {
            // An earlier version might have stored the token as a string
            String expiresAtSt = null;
            try {
                expiresAtSt = prefs.getString(PreferenceUtils.PREF_TOKEN_EXPIRES_AT, null);
                if (expiresAtSt != null) {
                    expiresAtMs = Long.parseLong(expiresAtSt);
                }
            } catch (NumberFormatException nfe) {
                // The string was not even a valid one, we'll ignore it.
                Log.w(LOGTAG, "pref_token_expires_at stored as invalid String : '" + expiresAtSt + "'", nfe);
            }
        }

        if (accessToken != null && expiresAtMs != null && expiresAtMs != 0) {
            Date expiresAt = new Date(expiresAtMs);
            auth = new Authentication();
            auth.setAccessToken(accessToken);
            auth.setExpirationDate(expiresAt);
        }
    }

    // Preferences

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            break;
        }

        return true;
    }

    @Override
    public void OnLoginChanged(boolean logged) {
    }

    class TabsPagerAdapter extends FragmentPagerAdapter {

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);

        }

        @Override
        public Fragment getItem(int index) {

            switch (index) {
            case 0: {
                return makeHomeFragment();
            }
            case 1:
                return Fragment.instantiate(MainActivity.this, RunFragment.class.getName());
            case 2:
                return makeConfigureFragment();
            }

            return null;
        }

        @Override
        public int getCount() {
            if (!isLogged()) {
                return 1;
            } else {
                return 3;
            }
        }

        protected HomeFragment makeHomeFragment() {
            HomeFragment fragment = (HomeFragment) Fragment
                    .instantiate(MainActivity.this, HomeFragment.class.getName());
            fragment.setTaskFactory(taskFactory);
            return fragment;
        }

        protected AvPhoneFragment makeConfigureFragment() {
            ConfigureFragment fragment = (ConfigureFragment) Fragment.instantiate(MainActivity.this,
                    ConfigureFragment.class.getName());
            fragment.setTaskFactory(taskFactory);
            return fragment;
        }

    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    public boolean isLogged() {
        return (this.auth != null && !this.auth.isExpired(new Date()));
    }

    @Override
    public void onAuthentication(Authentication auth) {

        this.auth = auth;

        saveAuthenticationInPreferences(auth);

        showLoggedTabs();

    }

    @Override
    public Authentication getAuthentication() {
        return this.auth;
    }

    private void saveAuthenticationInPreferences(Authentication auth) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        prefs.edit().putString(PreferenceUtils.PREF_ACCESS_TOKEN, auth.getAccessToken())
                .putLong(PreferenceUtils.PREF_TOKEN_EXPIRES_AT, auth.getExpirationDate().getTime()).commit();
    }

    public void showLoggedTabs() {
        if (actionBar.getTabCount() == 1) {
            actionBar.addTab(actionBar.newTab().setText(getString(R.string.run_tab)).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(getString(R.string.configure_tab)).setTabListener(this));
        }
        this.tabsPageAdapter.notifyDataSetChanged();
        this.viewPager.setCurrentItem(1);
    }

    public void hideLoggedTabs() {
        if (actionBar.getTabCount() == 3) {
            actionBar.removeTabAt(2);
            actionBar.removeTabAt(1);
        }
        this.tabsPageAdapter.notifyDataSetChanged();
        this.viewPager.setCurrentItem(0);
    }

    public void forgetAuthentication() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        prefs.edit().remove(PreferenceUtils.PREF_ACCESS_TOKEN).remove(PreferenceUtils.PREF_TOKEN_EXPIRES_AT).commit();

        this.auth = null;

        hideLoggedTabs();
        
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
            if (monitoringServiceListener != null) {
                monitoringServiceListener.onServiceStopped(monitoringService);
            }
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


}
