package net.airvantage.utils

import android.util.Log
import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient
import net.airvantage.model.*
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.utils.alert.AlertAdapterFactory
import net.airvantage.utils.alert.DefaultAlertAdapter
import net.airvantage.utils.alert.IAlertAdapterFactoryListener
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class AirVantageClient(private val server: String, private val access_token: String) : IAirVantageClient, IAlertAdapterFactoryListener {
    private var alertAdapter: DefaultAlertAdapter? = null
    private val gson: Gson = Gson()
    private val client: OkHttpClient = OkHttpClient()
    init {
        AlertAdapterFactory(server, access_token, this)
    }

    override fun alertAdapterAvailable(adapter: DefaultAlertAdapter) {
        alertAdapter = adapter
    }

    private fun buildPath(api: String): String {
        return server + API_PREFIX + api + "?access_token=" + access_token
    }

    private fun buildEndpoint(api: String): String {
        return SCHEME + buildPath(api)
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun readResponse(connection: HttpURLConnection): InputStream {
        val inputStream: InputStream
        when {
            connection.responseCode == HttpURLConnection.HTTP_OK -> inputStream = connection.inputStream
            connection.responseCode == HttpURLConnection.HTTP_BAD_REQUEST -> {
                inputStream = connection.errorStream
                val isr = InputStreamReader(inputStream)

                val error = gson.fromJson(isr, net.airvantage.model.AvError::class.java)

                Log.e(AirVantageClient::class.java.name, "AirVantage Error : " + error.error + "," + error.errorParameters)

                throw AirVantageException(error)
            }
            connection.responseCode == HttpURLConnection.HTTP_FORBIDDEN -> {
                val method = connection.requestMethod
                val url = connection.url.toString()
                val error = AvError(AvError.FORBIDDEN, Arrays.asList(method, url))
                throw AirVantageException(error)
            }
            else -> throw IOException("Unexpected HTTP response: " + connection.responseCode + " "
                    + connection.responseMessage)
        }
        return inputStream
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun sendString(method: String, url: URL, bodyString: String): InputStream {
        Log.d("**********",  url.toString())

        var out: OutputStream? = null
        try {

            // Create request for remote resource.
            val connection = client.open(url)
            connection.addRequestProperty("Cache-Control", "no-cache")
            connection.addRequestProperty("Content-Type", "application/json")

            // Write the request.
            connection.requestMethod = method
            out = connection.outputStream
            out!!.write(bodyString.toByteArray())
            return readResponse(connection)

        } finally {
            if (out != null)
                out.close()
        }
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun post(url: URL, body: Any): InputStream {
        val bodyString = gson.toJson(body)
        return sendString("POST", url, bodyString)
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun put(url: URL, body: Any): InputStream {
        val bodyString = gson.toJson(body)
        return sendString("PUT", url, bodyString)
    }

    @Throws(IOException::class, AirVantageException::class)
    private fun delete(url: URL) {


        val connection = client.open(url)

        connection.addRequestProperty("Cache-Control", "no-cache")
        connection.requestMethod = "DELETE"
        readResponse(connection)
    }

    @Throws(IOException::class, AirVantageException::class)
    private
    operator fun get(url: URL): InputStream? {
        // Create request for remote resource.
        val connection = client.open(url)
        connection.addRequestProperty("Cache-Control", "no-cache")

        return readResponse(connection)
    }

    override val currentUser: User
        get()  {
            val url = URL(buildEndpoint("/users/current"))
            val inputStream = get(url)
            return gson.fromJson(InputStreamReader(inputStream!!), User::class.java)
        }



    @Throws(IOException::class, AirVantageException::class)
    fun getSystemsBySerialNumber(serialNumber: String?): List<net.airvantage.model.AvSystem>? {
        var urlString = buildEndpoint("/systems") + "&fields=uid,name,commStatus,lastCommDate,data,applications,gateway,type"
        if (serialNumber != null) {
            urlString += "&gateway=serialNumber:" + serialNumber
        }
        val url = URL(urlString)
        val inputStream = this[url]
        return gson.fromJson(InputStreamReader(inputStream!!), SystemsList::class.java).items
    }

    @Throws(IOException::class, AirVantageException::class)
    fun getGateway(serialNumber: String): Boolean? {
        val urlString = buildEndpoint("/gateways")
        val url = URL(urlString)
        val inputStream = this[url]
        val reader = BufferedReader(InputStreamReader(inputStream!!))
        val sb = StringBuilder()
        var line = reader.readLine()

        while (line != null) {
            sb.append(line)
            line = reader.readLine()
        }
        inputStream.close()

        try {
            val json = JSONObject(sb.toString())
            val jsonValues = json.getJSONArray("items")
            for (i in 0 until jsonValues.length()) {
                val entry = jsonValues.getJSONObject(i)
                val name = entry.getString("serialNumber")
                if (name == serialNumber) {
                    return true
                }
            }
        } catch (e: JSONException) {
            return false
        }

        return false
    }

    @Throws(IOException::class, AirVantageException::class)
    fun createSystem(system: net.airvantage.model.AvSystem): net.airvantage.model.AvSystem {
        val url = URL(buildEndpoint("/systems"))
        val inputStream = post(url, system)
        return gson.fromJson(InputStreamReader(inputStream), net.airvantage.model.AvSystem::class.java)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun updateSystem(system: AvSystem) {
        val url = URL(buildEndpoint("/systems/" + system.uid!!))
        put(url, system)
    }


    @Throws(IOException::class, AirVantageException::class)
    fun deleteSystem(system: net.airvantage.model.AvSystem) {
        val url = URL(buildEndpoint("/systems/" + system.uid!!) + "&deleteGateway=true")
        delete(url)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun getApplications(appType: String): List<net.airvantage.model.Application>? {
        val url = URL(buildEndpoint("/applications") + "&type=" + appType + "&fields=uid,name,revision,type,category")
        val inputStream = this[url]
        inputStream.use { inStream ->
            return gson.fromJson(InputStreamReader(inStream!!), ApplicationsList::class.java).items
        }
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createApplication(application: net.airvantage.model.Application): net.airvantage.model.Application {
        val url = URL(buildEndpoint("/applications"))
        val inputStream = post(url, application)
        return gson.fromJson(InputStreamReader(inputStream), net.airvantage.model.Application::class.java)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun setApplicationData(applicationUid: String, data: List<ApplicationData>) {
        val url = URL(buildEndpoint("/applications/$applicationUid/data"))
        put(url, data)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun setApplicationCommunication(applicationUid: String, protocols: List<Protocol>) {
        val url = URL(buildEndpoint("/applications/$applicationUid/communication"))
        put(url, protocols)
    }

    @Throws(AirVantageException::class)
    private fun checkAlertAdapter() {
        if (this.alertAdapter == null)
            throw AirVantageException(AvError("Response pending"))
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun getAlertRuleByName(name: String, system: AvSystem): AlertRule? {
        checkAlertAdapter()
        return this.alertAdapter!!.getAlertRuleByName(name, system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createAlertRule(alertRule: AlertRule, application: String, system: AvSystem) {
        checkAlertAdapter()
        this.alertAdapter!!.createAlertRule(alertRule, application, system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun deleteAlertRule(alertRule: AlertRule) {
        checkAlertAdapter()
        this.alertAdapter!!.deleteAlertRule(alertRule)
    }


    @Throws(IOException::class, AirVantageException::class)
    override fun logout() {
        val url = URL(SCHEME + server + "/api/oauth/expire?access_token=" + access_token)
        get(url)
    }

    override val userRights: UserRights
        get() {
            val url = URL(buildEndpoint("/users/rights"))
            val inputStream = get(url)
            return gson.fromJson(InputStreamReader(inputStream!!), UserRights::class.java)
        }

    companion object {
        private const val SCHEME = "https://"
        private const val API_PREFIX = "/api/v1"

        fun buildImplicitFlowURL(server: String, clientId: String): String {
            return (SCHEME + server + "/api/oauth/authorize?client_id=" + clientId
                    + "&response_type=token&redirect_uri=oauth://airvantage")
        }
    }
}
