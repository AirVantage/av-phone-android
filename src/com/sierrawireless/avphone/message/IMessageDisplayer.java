package com.sierrawireless.avphone.message;

import android.text.Spanned;

/**
 * Generic interface for anything that can display success / error messages.
 */
public interface IMessageDisplayer {
    
    public void showError(int id, Object... params);

    public void showSuccess(int id, Object... params);

    public void showErrorMessage(Spanned spanned);
}
