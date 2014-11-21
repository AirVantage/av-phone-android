package com.sierrawireless.avphone.message;

/**
 * Generic interface for anything that can display success / error messages.
 */
public interface IMessageDisplayer {
    
    public void showError(int id, Object... params);

    public void showSuccess(int id, Object... params);
}
