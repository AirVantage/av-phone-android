package com.sierrawireless.avphone.task;

import com.sierrawireless.avphone.R;

public enum GetUserProgress {
    // There is no "GetUser@', since it is done
    // in the single 'ensureApplicationExists' method
    CHECKING_RIGHTS(R.string.progress_checking_rights, 0),
    GET_USER(R.string.progress_getting_user, 1),
    DONE(R.string.progress_done, 2);

    public final int stringId;
    public final int value;
    GetUserProgress(int stringId, int value) {
        this.stringId = stringId;
        this.value = value;
    }

}