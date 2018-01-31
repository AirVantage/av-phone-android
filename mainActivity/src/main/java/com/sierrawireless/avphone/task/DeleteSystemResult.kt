package com.sierrawireless.avphone.task

import net.airvantage.model.AvError
import net.airvantage.model.User

class DeleteSystemResult {

    var error: AvError? = null

    var user: User? = null

    val isError: Boolean
        get() = error != null

    internal constructor(error: AvError) {
        this.error = error
    }

    internal constructor(user: User) {
        this.user = user
    }
}
