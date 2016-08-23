package com.sierrawireless.avphone;

import android.app.Activity;
import android.content.Intent;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sierrawireless.avphone.auth.AuthenticationManager;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;

public abstract class AvPhoneFragment extends Activity implements IMessageDisplayer {

    protected AuthenticationManager authManager;

    protected SyncWithAvListener syncListener;
    protected IAsyncTaskFactory taskFactory;

    public AvPhoneFragment() {
        super();
    }

    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        if (this instanceof AuthenticationManager) {
            setAuthManager((AuthenticationManager) this);
        }

        if (this instanceof SyncWithAvListener) {
            this.syncListener = (SyncWithAvListener) this;
        }
    }

    protected void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    protected void initTaskFactory() {
        final MainActivity parent = (MainActivity) getParent();
        if (parent != null) {
            final IAsyncTaskFactory factory = ((MainActivity) getParent()).getTaskFactory();
            if (factory != null) {
                setTaskFactory(factory);
            }
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
        showErrorMessage(getString(id, params));
    }

    public void showErrorMessage(String message) {
        TextView errorMessageView = getErrorMessageView();
        errorMessageView.setText(message);
        errorMessageView.setVisibility(View.VISIBLE);
    }

    public void showErrorMessage(Spanned spanned) {
        TextView errorMessageView = getErrorMessageView();
        errorMessageView.setText(spanned);
        errorMessageView.setVisibility(View.VISIBLE);
    }
    
    public void hideErrorMessage() {
        getErrorMessageView().setVisibility(View.GONE);
    }

    private void toast(int id) {
        toast(getString(id));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void requestAuthentication() {
        Intent intent = new Intent(this, AuthorizationActivity.class);
        this.startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
    }

    protected abstract TextView getErrorMessageView();

}