package com.sierrawireless.avphone.message;

import android.text.Spanned;

/**
 * Generic interface for anything that can display success / error messages.
 */
public interface IMessageDisplayer {
    
    void showError(int id, Object... params);

    void showSuccess(int id, Object... params);

    void showErrorMessage(Spanned spanned);
}
