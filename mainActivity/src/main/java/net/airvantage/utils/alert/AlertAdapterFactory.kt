package net.airvantage.utils.alert

import android.os.AsyncTask
import android.util.Log

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.OkUrlFactory

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.HashMap

class AlertAdapterFactory(private val server: String, private val accessToken: String, private val listener: IAlertAdapterFactoryListener) : AsyncTask<Void, Void, DefaultAlertAdapter>() {
    private val client: OkHttpClient = OkHttpClient()
    private val TAG = this.javaClass.name


    override fun doInBackground(vararg params: Void): DefaultAlertAdapter {

        Log.d("ALERT", "CHECK ALERT ADAPTER")
        // Mapping of _Alert APIs_ -> Functions handling them set V2 in first preferred
        val urls = HashMap<String, DefaultAlertAdapter>()
        urls[ALERT_V2_API_PREFIX] = AlertAdapterV2(server, accessToken)
        urls[ALERT_V1_API_PREFIX] = AlertAdapterV1(server, accessToken)
        val founds = HashMap<String, Boolean>()

        // We using first available
        for ((key) in urls) {
            try {

                val urlString = "https://" + server + key + accessToken
                val url = URL(urlString)
                val connection = OkUrlFactory(client).open(url)
                connection.requestMethod = "GET"
                founds[key] = connection.responseCode == HttpURLConnection.HTTP_OK

            } catch (e: MalformedURLException) {
                Log.e(TAG, "Bad Url generated for " + key, e)
            } catch (e: IOException) {
                Log.e(TAG, "Connection problem", e)
            }
        }

        if (founds[ALERT_V2_API_PREFIX] != null && founds[ALERT_V2_API_PREFIX]!!) {
            return urls[ALERT_V2_API_PREFIX]!!
        } else if (founds[ALERT_V1_API_PREFIX] != null && founds[ALERT_V1_API_PREFIX]!!) {
            return urls[ALERT_V1_API_PREFIX]!!
        }

        // Neither is available?
        // This adapter provides error messages at each call
        Log.d("ALERT", "CHECK ALERT ADAPTER DONE")
        return DefaultAlertAdapter(server, accessToken)

    }

    override fun onPostExecute(adapter: DefaultAlertAdapter) {
        this.listener.alertAdapterAvailable(adapter)
    }

    companion object {
        private const val ALERT_V1_API_PREFIX = "/api/v1/alerts/rules?size=0&access_token="
        private const val ALERT_V2_API_PREFIX = "/api/v2/alertrules?access_token="
    }
}
