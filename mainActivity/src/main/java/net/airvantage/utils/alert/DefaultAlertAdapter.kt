package net.airvantage.utils.alert

import android.util.Log

import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient

import net.airvantage.model.AirVantageException
import net.airvantage.model.AvError
import net.airvantage.model.alert.v1.AlertRule

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Arrays

open class DefaultAlertAdapter internal constructor(protected val server: String, internal val access_token: String) {
    internal val gson: Gson = Gson()
    protected val client: OkHttpClient = OkHttpClient()
    private val TAG = this.javaClass.name

    open val prefix: String
        get() = "Override me"

    @Throws(IOException::class, AirVantageException::class)
    open fun createAlertRule(alertRule: AlertRule, application: String) {
        throw AirVantageException(AvError(AvError.FORBIDDEN))
    }

    @Throws(IOException::class, AirVantageException::class)
    open fun getAlertRuleByName(name: String, application: String): AlertRule? {
        throw AirVantageException(AvError(AvError.FORBIDDEN))
    }

    //
    // After alert v2 migration, everything below this line HAVE TO GO: it is DUPLICATION.
    //

    private fun buildPath(api: String): String {
        return server + prefix + api + "?access_token=" + access_token
    }

    internal fun buildEndpoint(api: String): String {
        return "https://" + buildPath(api)
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun readResponse(connection: HttpURLConnection): InputStream {
        val inp: InputStream
        when {
            connection.responseCode == HttpURLConnection.HTTP_OK -> {
                inp = connection.inputStream
                Log.i(TAG, "Just read : " + connection.url.toString()
                        + " with status " + HttpURLConnection.HTTP_OK)
            }
            connection.responseCode == HttpURLConnection.HTTP_BAD_REQUEST -> {
                inp = connection.errorStream
                val isr = InputStreamReader(inp)

                val error = gson.fromJson(isr, net.airvantage.model.AvError::class.java)
                Log.e(TAG, "AirVantage Error : " + error.error + "," + error.errorParameters)

                throw AirVantageException(error)
            }
            connection.responseCode == HttpURLConnection.HTTP_FORBIDDEN -> {
                val method = connection.requestMethod
                val url = connection.url.toString()
                val error = AvError(AvError.FORBIDDEN, Arrays.asList(method, url))
                throw AirVantageException(error)
            }
            else -> {
                val message = ("Reading " + connection.url.toString()
                        + " got unexpected HTTP response " + connection.responseCode + ", "
                        + connection.responseMessage)
                val ioException = IOException(message)
                Log.e(TAG, message, ioException)
                throw ioException
            }
        }
        return inp
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun sendString(method: String, url: URL, bodyString: String): InputStream {
        var out: OutputStream? = null
        try {

            // Create request for remote resource.
            val connection = client.open(url)
            connection.addRequestProperty("Cache-Control", "no-cache")
            connection.addRequestProperty("Content-Type", "application/json")
            // Write the request.
            connection.requestMethod = method

            val message = method + " on " + url.toString() + "\n" + bodyString
            out = connection.outputStream
            out!!.write(bodyString.toByteArray())
            return readResponse(connection)
        } finally {
            if (out != null)
                out.close()

        }

    }

    @Throws(IOException::class, AirVantageException::class)
    internal fun post(url: URL, body: Any): InputStream {
        val bodyString = gson.toJson(body)
        return sendString("POST", url, bodyString)
    }

    @Throws(IOException::class, AirVantageException::class)
    protected operator fun get(url: URL): InputStream {
        // Create request for remote resource.
        val connection = client.open(url)
        connection.addRequestProperty("Cache-Control", "no-cache")

        return readResponse(connection)
    }
}
