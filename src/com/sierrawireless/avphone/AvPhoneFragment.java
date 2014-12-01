package com.sierrawireless.avphone;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sierrawireless.avphone.auth.AuthenticationManager;
import com.sierrawireless.avphone.message.IMessageDisplayer;

public abstract class AvPhoneFragment extends Fragment implements IMessageDisplayer {

    protected AuthenticationManager authManager;

    public AvPhoneFragment() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof AuthenticationManager) {
            setAuthManager((AuthenticationManager) activity);
        }

    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }
    
    @Override
    public void showError(int id, Object... params) {
        this.showErrorMessage(id, params);
    }

    @Override
    public void showSuccess(int id, Object... params) {
        this.hideErrorMessage();
        this.toast(id);
    }

    public void showErrorMessage(int id, Object... params) {
        showErrorMessage(getActivity().getString(id, params));
    }

    public void showErrorMessage(String message) {
        TextView errorMessageView = getErrorMessageView();
        errorMessageView.setText(message);
        errorMessageView.setVisibility(View.VISIBLE);
    }

    public void hideErrorMessage() {
        getErrorMessageView().setVisibility(View.GONE);
    }

    private void toast(int id) {
        toast(getActivity().getString(id));
    }

    private void toast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    protected void requestAuthentication() {
        Intent intent = new Intent(this.getActivity(), AuthorizationActivity.class);
        this.startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
    }

    protected abstract TextView getErrorMessageView();

}