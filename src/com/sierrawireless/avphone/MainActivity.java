package com.sierrawireless.avphone;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.sierrawireless.avphone.auth.AuthenticationManager;
import com.sierrawireless.avphone.auth.IAuthenticationManager;
import com.sierrawireless.avphone.task.AsyncTaskFactory;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;

/**
 * The main activity, in charge of displaying the tab view
 */
public class MainActivity extends FragmentActivity implements TabListener, LoginListener {

    private ViewPager viewPager;
    private TabsPagerAdapter tabsPageAdapter;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        tabsPageAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(tabsPageAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

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
        if (logged) {
            if (actionBar.getTabCount() == 1) {
                actionBar.addTab(actionBar.newTab().setText(getString(R.string.run_tab)).setTabListener(this));
                actionBar.addTab(actionBar.newTab().setText(getString(R.string.configure_tab)).setTabListener(this));
            }
            this.tabsPageAdapter.setLogged(logged);
            this.tabsPageAdapter.notifyDataSetChanged();
            this.viewPager.setCurrentItem(1);
        } else {
            if (actionBar.getTabCount() == 3) {
                actionBar.removeTabAt(2);
                actionBar.removeTabAt(1);
            }
            this.tabsPageAdapter.setLogged(false);
            this.tabsPageAdapter.notifyDataSetChanged();
            this.viewPager.setCurrentItem(0);
        }
    }

    class TabsPagerAdapter extends FragmentPagerAdapter {

        public boolean logged = false;

        private IAsyncTaskFactory taskFactory;
        private IAuthenticationManager authManager;

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);

            taskFactory = new AsyncTaskFactory(MainActivity.this);
            authManager = new AuthenticationManager();

        }

        public void setLogged(boolean logged) {
            this.logged = logged;
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
            if (!logged) {
                return 1;
            } else {
                return 3;
            }
        }

        protected HomeFragment makeHomeFragment() {
            HomeFragment fragment = (HomeFragment) Fragment
                    .instantiate(MainActivity.this, HomeFragment.class.getName());
            fragment.setTaskFactory(taskFactory);
            fragment.setAuthenticationManager(authManager);
            fragment.addLoginListener(MainActivity.this);
            return fragment;
        }

        protected AvPhoneFragment makeConfigureFragment() {
            ConfigureFragment fragment = (ConfigureFragment) Fragment.instantiate(MainActivity.this,
                    ConfigureFragment.class.getName());
            fragment.setTaskFactory(taskFactory);
            fragment.setAuthenticationManager(authManager);
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

}
