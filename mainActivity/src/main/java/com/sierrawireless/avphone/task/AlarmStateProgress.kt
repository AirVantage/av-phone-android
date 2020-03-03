package com.sierrawireless.avphone.task

import com.sierrawireless.avphone.R

enum class AlarmStateProgress(val stringId: Int, val value: Int) {
    // There is no "CREATING_SYSTEM', since it is done
    // in the single 'ensureApplicationExists' method
    CHECKING_SYSTEM(R.string.progress_checking_system, 0),
    CHECKING_ALERT_RULE(R.string.progress_checking_alert_rule, 1),
    GETTING_ALERT_STATE(R.string.progress_getting_alert_state, 2),
    DONE(R.string.progress_done, 3)
}