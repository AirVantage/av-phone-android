package net.airvantage.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.ApplicationsList;
import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import net.airvantage.model.Protocol;
import net.airvantage.model.SystemsList;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;
import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.utils.alert.AlertAdapterFactory;
import net.airvantage.utils.alert.IAlertAdapterFactoryListener;
import net.airvantage.utils.alert.DefaultAlertAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class AirVantageClient implements IAirVantageClient, IAlertAdapterFactoryListener {
    private static final String TAG = "AirVantageClient";

    private static final String SCHEME = "https://";

    private static final String API_PREFIX = "/api/v1";

    private final String access_token;

    private DefaultAlertAdapter alertAdapter = null;

    private final String server;

    private Gson gson;

    private OkHttpClient client;

    public AirVantageClient(String server, String token) {
        this.server = server;
        this.access_token = token;
        this.gson = new Gson();
        this.client = new OkHttpClient();
        new AlertAdapterFactory(server, token, this);

    }

    @Override
    public void alertAdapterAvailable(DefaultAlertAdapter adapter) {
        alertAdapter = adapter;
    }

    public static String buildImplicitFlowURL(String server, String clientId) {
        return SCHEME + server + "/api/oauth/authorize?client_id=" + clientId
                + "&response_type=token&redirect_uri=oauth://airvantage";
    }

    private String buildPath(String api) {
        return server + API_PREFIX + api + "?access_token=" + access_token;
    }

    private String buildEndpoint(String api) {
        return SCHEME + buildPath(api);
    }

    private InputStream readResponse(HttpURLConnection connection) throws IOException, AirVantageException {
        InputStream in;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            in = connection.getInputStream();
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            in = connection.getErrorStream();
            InputStreamReader isr = new InputStreamReader(in);

            net.airvantage.model.AvError error = gson.fromJson(isr, net.airvantage.model.AvError.class);

            Log.e(AirVantageClient.class.getName(), "AirVantage Error : " + error.error + "," + error.errorParameters);

            throw new AirVantageException(error);
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
            String method = connection.getRequestMethod();
            String url = connection.getURL().toString();
            AvError error = new AvError(AvError.FORBIDDEN, Arrays.asList(method, url));
            throw new AirVantageException(error);
        } else {
            throw new IOException("Unexpected HTTP response: " + connection.getResponseCode() + " "
                    + connection.getResponseMessage());
        }
        return in;
    }

    private InputStream sendString(String method, URL url, String bodyString)
            throws IOException, AirVantageException {


        OutputStream out = null;
        try {

            // Create request for remote resource.
            HttpURLConnection connection = client.open(url);
            connection.addRequestProperty("Cache-Control", "no-cache");
            connection.addRequestProperty("Content-Type", "application/json");

            // Write the request.
            connection.setRequestMethod(method);
            out = connection.getOutputStream();
            out.write(bodyString.getBytes());
            return readResponse(connection);

        } finally {
            if (out != null)
                out.close();
        }

    }

    private InputStream post(URL url, Object body) throws IOException, AirVantageException {
        String bodyString = gson.toJson(body);
        return sendString("POST", url, bodyString);
    }

    protected InputStream put(URL url, Object body) throws IOException, AirVantageException {
        String bodyString = gson.toJson(body);
        Log.d(TAG, "put: body string is " + bodyString);
        return sendString("PUT", url, bodyString);
    }

    private void delete(URL url) throws IOException, AirVantageException {


        HttpURLConnection connection = client.open(url);

        connection.addRequestProperty("Cache-Control", "no-cache");
        connection.setRequestMethod("DELETE");
        readResponse(connection);
    }

    protected InputStream get(URL url) throws IOException, AirVantageException {
        // Create request for remote resource.
        HttpURLConnection connection = client.open(url);
        connection.addRequestProperty("Cache-Control", "no-cache");

        return readResponse(connection);
    }

    @Override
    public User getCurrentUser() throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/users/current"));
        InputStream in = get(url);
        return gson.fromJson(new InputStreamReader(in), User.class);
    }


    public List<net.airvantage.model.AvSystem> getSystemsBySerialNumber(String serialNumber)
            throws IOException, AirVantageException {
        String urlString = buildEndpoint("/systems")
                + "&fields=uid,name,commStatus,lastCommDate,data,applications,gateway,type";
        if (serialNumber != null) {
            urlString += "&gateway=serialNumber:" + serialNumber;
        }
        URL url = new URL(urlString);
        Log.d(TAG, "getSystemsBySerialNumber: url " + url);
        InputStream in = this.get(url);
        return gson.fromJson(new InputStreamReader(in), SystemsList.class).items;
    }

    public Boolean getGateway(String serialNumber) throws IOException, AirVantageException {
        String urlString = buildEndpoint("/gateways");
        Log.d(TAG, "getGateway: urlString " + urlString);
        URL url = new URL(urlString);
        InputStream in = this.get(url);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        in.close();

        try {
            JSONObject json = new JSONObject(sb.toString());
            JSONArray jsonValues = json.getJSONArray("items");
            for (int i = 0; i < jsonValues.length(); i++) {
                JSONObject entry = jsonValues.getJSONObject(i);
                String name = entry.getString("serialNumber");
                if (name.equals(serialNumber)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            return false;
        }
        return false;
    }

    public net.airvantage.model.AvSystem createSystem(net.airvantage.model.AvSystem system)
            throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/systems"));
        InputStream in = post(url, system);
        return gson.fromJson(new InputStreamReader(in), net.airvantage.model.AvSystem.class);
    }

    @Override
    public void updateSystem(AvSystem system) throws IOException, AirVantageException {
      //  Log.d(TAG, "updateSystem: system is " + system.uid);
        URL url = new URL(buildEndpoint("/systems/" + system.uid));
        put(url, system);
    }


    public void deleteSystem(net.airvantage.model.AvSystem system)  throws IOException, AirVantageException {
        //  Log.d(TAG, "updateSystem: system is " + system.uid);
        URL url = new URL(buildEndpoint("/systems/" + system.uid)+"&deleteGateway=true");
        delete(url);
    }
    public List<net.airvantage.model.Application> getApplications(String type) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/applications") + "&type=" + type + "&fields=uid,name,revision,type,category");
        InputStream in = this.get(url);
        try {
            return gson.fromJson(new InputStreamReader(in), ApplicationsList.class).items;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public net.airvantage.model.Application createApplication(net.airvantage.model.Application application)
            throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/applications"));
        InputStream in = post(url, application);
        return gson.fromJson(new InputStreamReader(in), net.airvantage.model.Application.class);
    }

    public void setApplicationData(String applicationUid, List<ApplicationData> data)
            throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/applications/" + applicationUid + "/data"));
        put(url, data);
    }

    public void setApplicationCommunication(String applicationUid, List<Protocol> protocols)
            throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/applications/" + applicationUid + "/communication"));
        put(url, protocols);
    }

    private void checkAlertAdapter() throws AirVantageException {
        if (this.alertAdapter == null)
            throw new AirVantageException(new AvError("Response pending"));
    }

    @Override
    public AlertRule getAlertRuleByName(final String name, String application) throws IOException, AirVantageException {
        checkAlertAdapter();
        return this.alertAdapter.getAlertRuleByName(name, application);
    }

    @Override
    public void createAlertRule(AlertRule alertRule, String application) throws IOException, AirVantageException {
        checkAlertAdapter();
        this.alertAdapter.createAlertRule(alertRule, application);
    }

    @Override
    public void logout() throws IOException, AirVantageException {
        URL url = new URL(SCHEME + server + "/api/oauth/expire?access_token=" + access_token);
        get(url);
    }

    public UserRights getUserRights() throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/users/rights"));
        InputStream in = get(url);
        return gson.fromJson(new InputStreamReader(in), UserRights.class);
    }
}
