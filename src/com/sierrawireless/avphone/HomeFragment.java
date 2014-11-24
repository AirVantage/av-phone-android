package com.sierrawireless.avphone;

import net.airvantage.model.AvError;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
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

import com.sierrawireless.avphone.auth.AuthUtils;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class HomeFragment extends AvPhoneFragment implements IMessageDisplayer {

    private static final String LOGTAG = HomeFragment.class.getName();

    private View view;

    private Button btnLogin;
    private Button btnLogout;
    private TextView instanceMessage;
    private TextView switchLink;

    private IAsyncTaskFactory taskFactory;

    public HomeFragment() {
        super();
    }

    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView welcome = (TextView) view.findViewById(R.id.home_welcome_message);
        welcome.setText(Html.fromHtml(getString(R.string.home_welcome_message)));
        getPrereqView().setText(Html.fromHtml(getString(R.string.home_before_starting)));

        btnLogin = (Button) view.findViewById(R.id.login_btn);

        btnLogout = (Button) view.findViewById(R.id.logout_btn);

        instanceMessage = (TextView) view.findViewById(R.id.switch_login_instance_text);

        switchLink = (TextView) view.findViewById(R.id.switch_instance);
        switchLink.setPaintFlags(switchLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        switchLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                PreferenceUtils.toggleServers(getActivity());

                chooseLoginMessage();

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

        chooseLoginMessage();

        if (authListener.isLogged()) {
            showLoggedInState();
        } else {
            showLoggedOutState();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        chooseLoginMessage();

        if (authListener.isLogged()) {
            showLoggedInState();
        } else {
            showLoggedOutState();
        }
    }
    
    private TextView getInfoMessageView() {
        TextView infoMessageView = (TextView) view.findViewById(R.id.home_info_message);
        return infoMessageView;
    }

    private void showCurrentServer() {
        AvPhonePrefs phonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());
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

    private void chooseLoginMessage() {
        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Authentication auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {

            authListener.onAuthentication(auth);

            syncWithAv(auth.getAccessToken());
        }
    }

    private void syncWithAv(String accessToken) {

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        final IMessageDisplayer displayer = this;
        final SyncWithAvTask syncAvTask = taskFactory.syncAvTask(avPhonePrefs.serverHost, accessToken);

        syncAvTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(AvError error) {
                if (error == null) {
                    showLoggedInState();
                } else {
                    authListener.forgetAuthentication();
                    showLoggedOutState();
                    syncAvTask.showResult(error, displayer, getActivity());
                }

            }
        });

        SyncWithAvParams params = new SyncWithAvParams();

        params.deviceId = DeviceInfo.getUniqueId(getActivity());
        params.imei = DeviceInfo.getIMEI(getActivity());
        params.mqttPassword = avPhonePrefs.password;
        params.customData = PreferenceUtils.getCustomDataLabels(getActivity());

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

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        String accessToken = authListener.getAuthentication().getAccessToken();
        
        AsyncTask<String, Integer, AvError> logoutTask = taskFactory.logoutTask(avPhonePrefs.serverHost, accessToken);

        logoutTask.execute();

        try {
            logoutTask.get();
        } catch (Exception e) {
            Log.w(LOGTAG, "Exception while ");
        } finally {
            // K authManager.forgetAuthentication(prefUtils);
            authListener.forgetAuthentication();

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

    // K should be listened by the MainActivity instead
    // @Override
    // public void onSharedPreferenceChanged(SharedPreferences prefs, String changedPrefKey) {
    // if (prefUtils.isMonitoringPreference(changedPrefKey)) {
    // // K authManager.forgetAuthentication(prefUtils);
    //
    // fireLoginChanged(false);
    // showLoggedOutState();
    // }
    // }

    public TextView getErrorMessageView() {
        return (TextView) view.findViewById(R.id.home_error_message);
    }

}
