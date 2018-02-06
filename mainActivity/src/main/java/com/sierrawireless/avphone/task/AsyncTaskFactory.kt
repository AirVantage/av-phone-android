package com.sierrawireless.avphone.task

import android.content.Context
import android.os.AsyncTask

import net.airvantage.model.AvError
import net.airvantage.utils.AirVantageClient

class AsyncTaskFactory(private val context: Context) : IAsyncTaskFactory {

    override fun syncAvTask(serverHost: String, token: String): SyncWithAvTask {

        val avClient = AirVantageClient(serverHost, token)

        val appClient = ApplicationClient(avClient)
        val systemClient = SystemClient(avClient)
        val alertRuleClient = AlertRuleClient(avClient)
        val userClient = UserClient(avClient)

        return ProgressSyncWithAvTask(appClient, systemClient, alertRuleClient, userClient, context)
    }

    override fun getUserTak(serverHost: String, token: String): GetUserTask {

        val avClient = AirVantageClient(serverHost, token)

        val userClient = UserClient(avClient)

        return ProgressGetUserTask(userClient, context)
    }

    override fun deleteSystemTak(serverHost: String, token: String): DeleteSystemTask {

        val avClient = AirVantageClient(serverHost, token)
        val systemClient = SystemClient(avClient)

        val userClient = UserClient(avClient)

        return ProgressDeleteSystemTask(systemClient, userClient, context)
    }


    override fun logoutTask(serverHost: String, token: String): AsyncTask<String, Int, AvError> {

        val client = AirVantageClient(serverHost, token)

        return LogoutTask(client)
    }
}
