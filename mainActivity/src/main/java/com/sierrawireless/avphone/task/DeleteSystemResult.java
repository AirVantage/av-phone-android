package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import net.airvantage.model.User;

public class DeleteSystemResult {

    private AvError error;

    private User user;

    DeleteSystemResult(AvError error) {
        this.error = error;
    }

    DeleteSystemResult(final User user) {
        this.user = user;
    }

    public boolean isError() {
        return error != null;
    }

    public User getUser() {
        return user;
    }

    public AvError getError() {
        return error;
    }
}
