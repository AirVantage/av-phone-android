package com.sierrawireless.avphone.task

import com.sierrawireless.avphone.R

enum class UpdateProgress(val stringId: Int, val value: Int) {
    // There is no "CREATING_SYSTEM', since it is done
    // in the single 'ensureApplicationExists' method
    CHECKING_RIGHTS(R.string.progress_checking_rights, 0),
    CHECKING_APPLICATION(R.string.progress_checking_application, 1),
    CHECKING_SYSTEM(R.string.progress_checking_system, 2),
    UPDATING_SYSTEM(R.string.progress_updating_system, 3),
    CHECKING_ALERT_RULE(R.string.progress_checking_alert_rule, 4),
    UPDATING_ALERT_RULE(R.string.progress_updating_alert_rule, 5),
    UPDATING_APPLICATION(R.string.progress_updating_application, 6),
    ADDING_APPLICATION(R.string.progress_adding_application, 7),
    DONE(R.string.progress_done, 8)
}