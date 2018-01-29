package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import net.airvantage.model.User;

public class GetUserResult {

    private AvError error;

    private User user;

    GetUserResult(AvError error) {
        this.error = error;
    }

    GetUserResult(final User user) {
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
