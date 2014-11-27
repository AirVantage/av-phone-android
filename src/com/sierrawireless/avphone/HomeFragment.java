package com.sierrawireless.avphone;

import net.airvantage.model.AvError;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

    private Button btnLoginNa;
    private Button btnLoginEu;
    
    private Button btnLogout;
    
    private IAsyncTaskFactory taskFactory;

    private Button btnLoginCustom;

    public HomeFragment() {
        super();
    }

    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_home, container, false);

        btnLoginNa = (Button) view.findViewById(R.id.login_na_btn);
        btnLoginEu = (Button) view.findViewById(R.id.login_eu_btn);
        btnLoginCustom = (Button) view.findViewById(R.id.login_custom_btn);

        btnLogout = (Button) view.findViewById(R.id.logout_btn);

       btnLoginNa.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                PreferenceUtils.setServer(PreferenceUtils.Server.NA, getActivity());
                requestAuthentication();
            }
        });

        btnLoginEu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                PreferenceUtils.setServer(PreferenceUtils.Server.EU, getActivity());
                requestAuthentication();
            }
        });

        
        btnLoginEu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                PreferenceUtils.setServer(PreferenceUtils.Server.EU, getActivity());
                requestAuthentication();
            }
        });

        btnLoginCustom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAuthentication();
            }
        });
        
        btnLogout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                logout();
            }
        });

        if (authManager.isLogged()) {
            showLoggedInState();
        } else {
            showLoggedOutState();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (authManager.isLogged()) {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Authentication auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {

            authManager.onAuthentication(auth);

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
                    hideErrorMessage();
                    showLoggedInState();
                } else {
                    authManager.forgetAuthentication();
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
        showCurrentServer();
        showLogoutButton();
    }

    private void showLoggedOutState() {
        hideLogoutButton();
        hideCurrentServer();
    }

    private void logout() {

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        String accessToken = authManager.getAuthentication().getAccessToken();
        
        AsyncTask<String, Integer, AvError> logoutTask = taskFactory.logoutTask(avPhonePrefs.serverHost, accessToken);

        logoutTask.execute();

        try {
            logoutTask.get();
        } catch (Exception e) {
            Log.w(LOGTAG, "Exception while ");
        } finally {
            // K authManager.forgetAuthentication(prefUtils);
            authManager.forgetAuthentication();

            showLoggedOutState();
        }

    }

    private void showLogoutButton() {
        btnLogout.setVisibility(View.VISIBLE);
        btnLoginNa.setVisibility(View.GONE);
        btnLoginEu.setVisibility(View.GONE);
        btnLoginCustom.setVisibility(View.GONE);
        view.findViewById(R.id.home_login_message).setVisibility(View.GONE);
    }

    private void hideLogoutButton() {
        btnLogout.setVisibility(View.GONE);
        btnLoginNa.setVisibility(View.VISIBLE);
        btnLoginEu.setVisibility(View.VISIBLE);
        
        view.findViewById(R.id.home_login_message).setVisibility(View.VISIBLE);
        
        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());
        if (prefs.usesCustomServer()) {
            btnLoginCustom.setText(getActivity().getString(R.string.home_login_custom, prefs.serverHost));
            btnLoginCustom.setVisibility(View.VISIBLE);
        } else {
            btnLoginCustom.setVisibility(View.GONE);
        }
        

    }

    public TextView getErrorMessageView() {
        return (TextView) view.findViewById(R.id.home_error_message);
    }

}
