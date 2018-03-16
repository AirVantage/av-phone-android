package com.sierrawireless.avphone.task

import net.airvantage.model.AvError
import net.airvantage.model.AvSystem
import net.airvantage.model.User

class SyncWithAvResult {

    var error: AvError? = null

    var system: AvSystem? = null

    var user: User? = null

    val isError: Boolean
        get() = error != null

    constructor(error: AvError) {
        this.error = error
    }

    constructor(system: AvSystem, user: User) {
        this.system = system
        this.user = user
    }
}
