package com.sierrawireless.avphone.task

import net.airvantage.model.AvError
import android.os.AsyncTask

interface IAsyncTaskFactory {
    fun syncAvTask(serverHost: String, token: String): SyncWithAvTask
    fun alarmStateTask(serverHost: String, token: String): AlarmStateTask
    fun getUserTak(serverHost: String, token: String): GetUserTask
    fun deleteSystemTak(serverHost: String, token: String): DeleteSystemTask

    fun logoutTask(serverHost: String, token: String): AsyncTask<String, Int, AvError>

    fun updateTask(serverHost: String, token: String): UpdateTask
}
