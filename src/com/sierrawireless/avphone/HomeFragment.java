package com.sierrawireless.avphone;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.airvantage.model.AvError;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationListener;
import com.sierrawireless.avphone.auth.IAuthenticationManager;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class HomeFragment extends AvPhoneFragment implements OnSharedPreferenceChangeListener, AuthenticationListener,
        IMessageDisplayer {

    private static final String LOGTAG = HomeFragment.class.getName();

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

    public void setAuthenticationManager(IAuthenticationManager authManager) {
        this.authManager = authManager;
    }

    public void addLoginListener(LoginListener loginListener) {
        this.loginListeners.add(loginListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView welcome = (TextView) view.findViewById(R.id.home_welcome_message);
        welcome.setText(Html.fromHtml(getString(R.string.home_welcome_message)));
        getPrereqView().setText(Html.fromHtml(getString(R.string.home_before_starting)));

        prefUtils = new PreferenceUtils(this);
        prefUtils.addListener(this);

        btnLogin = (Button) view.findViewById(R.id.login_btn);

        btnLogout = (Button) view.findViewById(R.id.logout_btn);

        instanceMessage = (TextView) view.findViewById(R.id.switch_login_instance_text);

        switchLink = (TextView) view.findViewById(R.id.switch_instance);
        switchLink.setPaintFlags(switchLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
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
            fireLoginChanged(true);
            showLoggedInState();
        }

        return view;
    }

    private TextView getInfoMessageView() {
        TextView infoMessageView = (TextView) view.findViewById(R.id.home_info_message);
        return infoMessageView;
    }

    private void showCurrentServer() {
        AvPhonePrefs phonePrefs = prefUtils.getAvPhonePrefs();
        TextView infoMessageView = getInfoMessageView();

        String message = null;
        if (phonePrefs.usesNA()) {
            message = getString(R.string.logged_on_na);
        } else if (phonePrefs.usesEU()) {
            message = getString(R.string.logged_on_eu);
        } else {
            message = getString(R.string.logged_on_custom, phonePrefs.serverHost);
        }

        infoMessageView.setText(message);
        infoMessageView.setVisibility(View.VISIBLE);
    }

    private void hideCurrentServer() {
        TextView infoMessageView = getInfoMessageView();
        infoMessageView.setVisibility(View.GONE);
        infoMessageView.setText("");
    }

    private void hidePrerequesites() {
        TextView prereqView = getPrereqView();
        prereqView.setVisibility(View.GONE);
    }

    private TextView getPrereqView() {
        TextView prereqView = (TextView) view.findViewById(R.id.home_before_starting);
        return prereqView;
    }

    private void setLoginMessage() {
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
            switchLink.setVisibility(View.GONE);
        }
    }

    private void requestAuthentication() {
        authManager.authenticate(prefUtils, this, this);
    }

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

        final IMessageDisplayer displayer = this;
        final SyncWithAvTask syncAvTask = taskFactory.syncAvTask(avPhonePrefs.serverHost, accessToken);

        syncAvTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(AvError error) {
                if (error == null) {
                    fireLoginChanged(true);
                    showLoggedInState();
                } else {
                    authManager.forgetAuthentication(prefUtils);
                    fireLoginChanged(false);
                    showLoggedOutState();
                    syncAvTask.showResult(error, displayer, getActivity());
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

    private void showLoggedInState() {
        hidePrerequesites();
        showCurrentServer();
        showLogoutButton();
    }

    private void showLoggedOutState() {
        hideLogoutButton();
        hideCurrentServer();
    }

    private void logout() {

        AvPhonePrefs avPhonePrefs = prefUtils.getAvPhonePrefs();
        String accessToken = authManager.getAuthentication(prefUtils).getAccessToken();

        AsyncTask<String, Integer, AvError> logoutTask = taskFactory.logoutTask(avPhonePrefs.serverHost, accessToken);

        logoutTask.execute();

        try {
            logoutTask.get();
        } catch (Exception e) {
            Log.w(LOGTAG, "Exception while ");
        } finally {
            authManager.forgetAuthentication(prefUtils);
            fireLoginChanged(false);
            showLoggedOutState();
        }

    }

    private void showLogoutButton() {
        btnLogout.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.GONE);
        instanceMessage.setVisibility(View.GONE);
        switchLink.setVisibility(View.GONE);
    }

    private void hideLogoutButton() {
        btnLogout.setVisibility(View.GONE);
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
            showLoggedOutState();
        }
    }

    public TextView getErrorMessageView() {
        return (TextView) view.findViewById(R.id.home_error_message);
    }

}
