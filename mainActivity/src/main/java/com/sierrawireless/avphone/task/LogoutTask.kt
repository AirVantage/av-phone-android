package com.sierrawireless.avphone.task

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.AvError
import net.airvantage.utils.IAirVantageClient
import android.os.AsyncTask

class LogoutTask internal constructor(private val client: IAirVantageClient) : AsyncTask<String, Int, AvError>() {

    override fun doInBackground(vararg arg0: String): AvError? {
        var res: AvError? = null
        try {
            this.client.logout()
        } catch (e: IOException) {
            res = AvError("unexpected.error")
        } catch (e: AirVantageException) {
            res = e.error
        }

        return res
    }

}
