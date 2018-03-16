package com.sierrawireless.avphone.task

import com.sierrawireless.avphone.R

enum class DeleteSystemProgress(val stringId: Int, val value: Int) {
    // There is no "CREATING_SYSTEM', since it is done
    // in the single 'ensureApplicationExists' method
    CHECKING_RIGHTS(R.string.progress_checking_rights, 0),
    CHECKING_SYSTEM(R.string.progress_checking_system, 1),
    CHECKING_ALERTRULE(R.string.progress_checking_alert_rule, 2),
    DELETING_ALERTRULE(R.string.progress_deleting_alert_rule, 3),

    DELETING_SYSTEM(R.string.progress_deleting_system, 4),
    DONE(R.string.progress_done, 5)

}