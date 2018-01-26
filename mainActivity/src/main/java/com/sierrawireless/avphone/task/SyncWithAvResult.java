package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import net.airvantage.model.User;

public class SyncWithAvResult {

    private AvError error;

    private AvSystem system;

    private User user;

    SyncWithAvResult(AvError error) {
        this.error = error;
    }

    SyncWithAvResult(final AvSystem system, final User user) {
        this.system = system;
        this.user = user;
    }

    public boolean isError() {
        return error != null;
    }

    public AvSystem getSystem() {
        return system;
    }

    public User getUser() {
        return user;
    }

    public AvError getError() {
        return error;
    }
}
