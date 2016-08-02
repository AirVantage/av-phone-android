package net.airvantage.utils.alert;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.OkHttpClient;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvError;
import net.airvantage.model.alert.v1.AlertRule;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class DefaultAlertAdapter {

    protected final String access_token;
    protected final Gson gson;
    protected final OkHttpClient client;
    protected final String server;

    public DefaultAlertAdapter(String server, String token) {
        this.access_token = token;
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.server = server;
    }

    public AlertRule createAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        throw new AirVantageException(new AvError(AvError.FORBIDDEN));
    }

    public AlertRule updateAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        throw new AirVantageException(new AvError(AvError.FORBIDDEN));
    }

    public AlertRule getAlertRuleByName(String name) throws IOException, AirVantageException {
        throw new AirVantageException(new AvError(AvError.FORBIDDEN));
    }

    //
    // After alert v2 migration, everything below this line HAVE TO GO: it is DUPLICATION.
    //

    protected String buildPath(String api) {
        final String path = server + getPrefix() + api + "?access_token=" + access_token;
        Log.d(this.getClass().toString(), "About to call: " + path);
        return path;
    }

    protected String getPrefix() {
        return "Override me";
    }

    protected String buildEndpoint(String api) {
        return "https://" + buildPath(api);
    }

    protected InputStream readResponse(HttpURLConnection connection) throws IOException, AirVantageException {
        InputStream in;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            in = connection.getInputStream();
            Log.d(this.getClass().getName(), "Just read : " + connection.getURL().toString()
                    + " with status " + HttpURLConnection.HTTP_OK);
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            in = connection.getErrorStream();
            InputStreamReader isr = new InputStreamReader(in);

            net.airvantage.model.AvError error = gson.fromJson(isr, net.airvantage.model.AvError.class);
            Log.e(this.getClass().getName(), "AirVantage Error : " + error.error + "," + error.errorParameters);

            throw new AirVantageException(error);
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
            String method = connection.getRequestMethod();
            String url = connection.getURL().toString();
            AvError error = new AvError(AvError.FORBIDDEN, Arrays.asList(method, url));
            throw new AirVantageException(error);
        } else {
            String message = "Reading " + connection.getURL().toString()
                    + " got unexpected HTTP response " + connection.getResponseCode() + ", "
                    + connection.getResponseMessage();
            IOException ioException = new IOException(message);
            Log.e(this.getClass().getName(), message, ioException);
            throw ioException;
        }
        return in;
    }

    protected InputStream sendString(String method, URL url, String bodyString)
            throws IOException, AirVantageException {

        OutputStream out = null;
        try {

            // Create request for remote resource.
            HttpURLConnection connection = client.open(url);
            connection.addRequestProperty("Cache-Control", "no-cache");
            connection.addRequestProperty("Content-Type", "application/json");
            // Write the request.
            connection.setRequestMethod(method);

            final String message = method +" on " + url.toString() + "\n" + bodyString;
            Log.d(this.getClass().getName(), message);

            out = connection.getOutputStream();
            out.write(bodyString.getBytes());
            return readResponse(connection);
        } finally {
            if (out != null)
                out.close();

        }

    }

    protected InputStream post(URL url, Object body) throws IOException, AirVantageException {
        String bodyString = gson.toJson(body);
        return sendString("POST", url, bodyString);
    }

    protected InputStream put(URL url, Object body) throws IOException, AirVantageException {
        String bodyString = gson.toJson(body);
        return sendString("PUT", url, bodyString);
    }

    protected InputStream get(URL url) throws IOException, AirVantageException {
        // Create request for remote resource.
        HttpURLConnection connection = client.open(url);
        connection.addRequestProperty("Cache-Control", "no-cache");

        return readResponse(connection);
    }
}
