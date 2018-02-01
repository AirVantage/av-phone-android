package com.sierrawireless.avphone.task

import net.airvantage.model.AvError
import net.airvantage.model.User

class GetUserResult {

    var error: AvError? = null

    var user: User? = null

    val isError: Boolean
        get() = error != null

    constructor(error: AvError) {
        this.error = error
    }

    constructor(user: User) {
        this.user = user
    }
}
