package com.sierrawireless.avphone.task

import net.airvantage.model.AirVantageException
import net.airvantage.model.User

interface IUserClient {

    val user: User?

    @Throws(AirVantageException::class)
    fun checkRights(): List<String>
}
