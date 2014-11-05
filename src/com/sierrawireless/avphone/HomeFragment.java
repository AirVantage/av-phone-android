package com.sierrawireless.avphone;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.airvantage.model.AvError;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationListener;
import com.sierrawireless.avphone.auth.IAuthenticationManager;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class HomeFragment extends Fragment implements OnSharedPreferenceChangeListener, AuthenticationListener {

    private View view;

    private Button btnLogin;
    private Button btnLogout;
    private TextView instanceMessage;
    private TextView switchLink;

    private PreferenceUtils prefUtils;

    private IAsyncTaskFactory taskFactory;
    private IAuthenticationManager authManager;
    
    private List<LoginListener> loginListeners = new ArrayList<LoginListener>();

    public HomeFragment() {
        super();
    }

    protected HomeFragment(IAsyncTaskFactory taskFactory, IAuthenticationManager authManager) {
        super();
        assert (taskFactory != null);
        assert (authManager != null);
        this.taskFactory = taskFactory;
        this.authManager = authManager;
    }

    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public void setAuthManager(IAuthenticationManager authManager) {
        this.authManager = authManager;
    }
    
    public void addLoginListener(LoginListener loginListener) {
        this.loginListeners.add(loginListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_home, container, false);

        prefUtils = new PreferenceUtils(this);
        prefUtils.addListener(this);
        
        btnLogin = (Button) view.findViewById(R.id.login_btn);

        btnLogout = (Button) view.findViewById(R.id.logout_btn);

        instanceMessage = (TextView) view.findViewById(R.id.switch_login_instance_text);

        switchLink = (TextView) view.findViewById(R.id.switch_instance);
        switchLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                prefUtils.toggleServers();

                setLoginMessage();

                requestAuthentication();

            }

        });

        btnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                requestAuthentication();
            }
        });

        btnLogout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                logout();
            }
        });

        setLoginMessage();

        Authentication auth = authManager.getAuthentication(prefUtils);
        if (auth != null && !auth.isExpired(new Date())) {
            showLogoutButton();
            fireLoginChanged(true);
        }
        

        return view;
    }

    private void setLoginMessage() {
        // TODO(pht) set logout message ?
        AvPhonePrefs avPhonePrefs = prefUtils.getAvPhonePrefs();
        if (avPhonePrefs.usesNA()) {
            btnLogin.setText(getString(R.string.login_na));
            instanceMessage.setText(getString(R.string.switch_instance_eu));
            switchLink.setVisibility(View.VISIBLE);
        } else if (avPhonePrefs.usesEU()) {
            btnLogin.setText(getString(R.string.login_eu));
            instanceMessage.setText(getString(R.string.switch_instance_na));
            switchLink.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setText(getString(R.string.login_custom_server));
            instanceMessage.setText(getString(R.string.custom_instance, avPhonePrefs.serverHost));
            switchLink.setVisibility(View.INVISIBLE);
        }
    }


    private void requestAuthentication() {
        /*
        Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
        startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
        */
        authManager.authenticate(prefUtils,this, this);
    }

    // TODO(pht) factor ConfigureFragment
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Authentication auth = authManager.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {
            authManager.saveAuthentication(prefUtils, auth);
            onAuthentication(auth);
        }
    }
    
    @Override
    public void onAuthentication(Authentication auth) {
        syncWithAv(auth.getAccessToken());
    }
    
    private void syncWithAv(String accessToken) {

        AvPhonePrefs avPhonePrefs = prefUtils.getAvPhonePrefs();

        assert (taskFactory != null);

        SyncWithAvTask syncAvTask = taskFactory.syncAvTask(avPhonePrefs.serverHost, accessToken);

        syncAvTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(AvError error) {
                if (error == null) {
                    fireLoginChanged(true);
                    showLogoutButton();
                }

            }
        });

        SyncWithAvParams params = new SyncWithAvParams();

        params.deviceId = DeviceInfo.getUniqueId(getActivity());
        params.imei = DeviceInfo.getIMEI(getActivity());
        params.mqttPassword = avPhonePrefs.password;
        params.customData = prefUtils.getCustomDataLabels();

        syncAvTask.execute(params);

    }

    private void logout() {

        AvPhonePrefs avPhonePrefs = prefUtils.getAvPhonePrefs();
        String accessToken = authManager.getAuthentication(prefUtils).getAccessToken();
        
        AsyncTask<String, Integer, AvError> logoutTask = taskFactory.logoutTask(avPhonePrefs.serverHost, accessToken);

        logoutTask.execute();

        try {
            AvError error = logoutTask.get();
            if (error == null) {
                
                authManager.forgetAuthentication(prefUtils);
                fireLoginChanged(false);
                hideLogoutButton();
                
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void showLogoutButton() {
        btnLogout.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.INVISIBLE);
        instanceMessage.setVisibility(View.INVISIBLE);
        switchLink.setVisibility(View.INVISIBLE);
    }

    private void hideLogoutButton() {
        btnLogout.setVisibility(View.INVISIBLE);
        btnLogin.setVisibility(View.VISIBLE);
        instanceMessage.setVisibility(View.VISIBLE);
        switchLink.setVisibility(View.VISIBLE);
    }

    private void fireLoginChanged(boolean logged) {
        for (LoginListener listener : loginListeners) {
            listener.OnLoginChanged(logged);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String changedPrefKey) {
        if (prefUtils.isMonitoringPreference(changedPrefKey)) {
            authManager.forgetAuthentication(prefUtils);
            fireLoginChanged(false);
            hideLogoutButton();
        }
    }

}
