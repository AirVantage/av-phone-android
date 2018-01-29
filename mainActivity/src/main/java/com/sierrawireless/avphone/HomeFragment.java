package com.sierrawireless.avphone;

import net.airvantage.model.AvError;
import net.airvantage.model.User;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import android.app.Activity;
import android.content.Intent;
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

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.auth.AuthUtils;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.GetUserListener;
import com.sierrawireless.avphone.task.GetUserParams;
import com.sierrawireless.avphone.task.GetUserResult;
import com.sierrawireless.avphone.task.GetUserTask;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvResult;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class HomeFragment extends AvPhoneFragment implements IMessageDisplayer {

    private static final String LOGTAG = HomeFragment.class.getName();

    private View view;

    private Authentication authForSync;
    private boolean retrySync;

    private Button btnLogin;
    private Button btnLogout;

    private IAsyncTaskFactory taskFactory;

    private User user;

    public HomeFragment() {
        super();
        retrySync = false;
        taskFactory = null;
    }

    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
        if (retrySync) {
            retrySync = false;
            syncWithAv(authForSync);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        syncListener = (SyncWithAvListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView loginMessage = view.findViewById(R.id.home_login_message);
        loginMessage.setText(Html.fromHtml(getString(R.string.home_login_message)));

        btnLogin = view.findViewById(R.id.login_btn);

        btnLogout = view.findViewById(R.id.logout_btn);

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
        return (TextView) view.findViewById(R.id.home_info_message);
    }

    private void showCurrentServer() {
        AvPhonePrefs phonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());
        TextView infoMessageView = getInfoMessageView();

        String message;
        if (phonePrefs.usesNA()) {
            message = getString(R.string.logged_on_na);
        } else if (phonePrefs.usesEU()) {
            message = getString(R.string.logged_on_eu);
        } else {
            message = getString(R.string.logged_on_custom, phonePrefs.serverHost);
        }

        infoMessageView.setText(message);
        infoMessageView.setVisibility(View.VISIBLE);
        TextView welcome = view.findViewById(R.id.home_login);
        if(user != null) {
            welcome.setText(String.format("%s%s", getString(R.string.welcome), user.name));
            welcome.setVisibility(View.VISIBLE);
        }
    }

    private void hideCurrentServer() {
        TextView infoMessageView = getInfoMessageView();
        infoMessageView.setVisibility(View.GONE);
        TextView welcome = view.findViewById(R.id.home_login);
        welcome.setVisibility(View.GONE);
        infoMessageView.setText("");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Authentication auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {
            syncWithAv(auth);
        }
    }


    private void syncGetUser(final Authentication auth) {
        hideErrorMessage();

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        // Without task factory, try later
        if (taskFactory == null) {
            authForSync = auth;
            retrySync = true;
            return;
        }


        final IMessageDisplayer displayer = this;
        final GetUserTask getUserTask = taskFactory.getUserTak(avPhonePrefs.serverHost, auth.getAccessToken());

        getUserTask.addProgressListener(new GetUserListener() {
            @Override
            public void onGetting(GetUserResult result) {
                if (result.isError()) {
                    authManager.forgetAuthentication();
                    showLoggedOutState();
                    getUserTask.showResult(result, displayer, getActivity());
                } else {
                    authManager.onAuthentication(auth);
                    showLoggedInState();
                    user = result.getUser();
                }

            }
        });

        final GetUserParams params = new GetUserParams();

        getUserTask.execute(params);

    }

    private void syncWithAv(final Authentication auth) {

        hideErrorMessage();

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        // Without task factory, try later
        if (taskFactory == null) {
            authForSync = auth;
            retrySync = true;
            return;
        }

        final IMessageDisplayer displayer = this;
        final SyncWithAvTask syncAvTask = taskFactory.syncAvTask(avPhonePrefs.serverHost, auth.getAccessToken());

        syncAvTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(SyncWithAvResult result) {
                if (result.isError()) {
                    authManager.forgetAuthentication();
                    showLoggedOutState();
                    syncAvTask.showResult(result, displayer, getActivity());
                } else {
                    authManager.onAuthentication(auth);
                    showLoggedInState();
                    user = result.getUser();
                    syncListener.onSynced(result);
                }

            }
        });

        final SyncWithAvParams params = new SyncWithAvParams();

        params.deviceId = DeviceInfo.getUniqueId(getActivity());
        params.imei = DeviceInfo.getIMEI(getActivity());
        params.deviceName = DeviceInfo.getDeviceName();
        params.iccid = DeviceInfo.getICCID(getActivity());
        params.mqttPassword = avPhonePrefs.password;
        params.customData = PreferenceUtils.getCustomDataLabels(getActivity());
        //     params.current = ((MainActivity)getActivity()).current;
        params.activity = ((MainActivity) getActivity());

        syncAvTask.execute(params);

    }

    private void showLoggedInState() {
        if (user == null){
            syncGetUser(authManager.getAuthentication());
        }
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
            Log.w(LOGTAG, "Exception while logging out");
            Crashlytics.logException(e);
        } finally {
            authManager.forgetAuthentication();

            showLoggedOutState();
        }

    }

    private void showLogoutButton() {
        btnLogout.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.GONE);
        view.findViewById(R.id.home_login_message).setVisibility(View.GONE);
    }

    private void hideLogoutButton() {
        btnLogout.setVisibility(View.GONE);
        btnLogin.setVisibility(View.VISIBLE);

        view.findViewById(R.id.home_login_message).setVisibility(View.VISIBLE);
    }

    public TextView getErrorMessageView() {
        return (TextView) view.findViewById(R.id.home_error_message);
    }

}
