package net.airvantage.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Alert;
import net.airvantage.model.AlertRule;
import net.airvantage.model.AlertRuleList;
import net.airvantage.model.AlertsList;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.ApplicationsList;
import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import net.airvantage.model.Datapoint;
import net.airvantage.model.Protocol;
import net.airvantage.model.SystemsList;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;

public class AirVantageClient implements IAirVantageClient {

    private static final String SCHEME = "https://";

    private static final String APIS = "/api/v1";

    private final String access_token;

    private final String server;

    private Gson gson;

    private OkHttpClient client;

    public static String buildImplicitFlowURL(String server, String clientId) {
        return SCHEME + server + "/api/oauth/authorize?client_id=" + clientId
                + "&response_type=token&redirect_uri=oauth://airvantage";
    }

    public AirVantageClient(String server, String token) {
        this.server = server;
        this.access_token = token;
        this.gson = new Gson();
        this.client = new OkHttpClient();
    }

    private String buildPath(String api) {
        return server + APIS + api + "?access_token=" + access_token;
    }

    private String buildEndpoint(String api) {
        return SCHEME + buildPath(api);
    }

    protected InputStream readResponse(HttpURLConnection connection) throws IOException, AirVantageException {
        InputStream in = null;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            in = connection.getInputStream();
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            in = connection.getErrorStream();
            InputStreamReader isr = new InputStreamReader(in);

            // String wrf = asString(isr);

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

    @Override
    public User getCurrentUser() throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/users/current"));
        InputStream in = get(url);
        return gson.fromJson(new InputStreamReader(in), User.class);
    }

    public void expire() throws IOException, AirVantageException {
        URL url = new URL(server + "/api/oauth/expire?access_token=" + access_token);
        this.get(url);
    }

    public List<Alert> getUnacknowledgedAlerts(String systemUid) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/privates/alerts/groups") + "&acknowledged=false&target=" + systemUid);
        InputStream in = this.get(url);
        return gson.fromJson(new InputStreamReader(in), AlertsList.class).items;
    }

    public List<Datapoint> getLast24Hours(String systemUid, String data) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/systems/" + systemUid + "/data/" + data + "/aggregated") + "&fn=mean&from="
                + (System.currentTimeMillis() - 23 * 60 * 60 * 1000) + "&to="
                + (System.currentTimeMillis() + 1 * 60 * 60 * 1000));

        InputStream in = this.get(url);

        // Deserialize HTTP response to concrete type.
        Type collectionType = new TypeToken<List<Datapoint>>() {
        }.getType();
        return gson.fromJson(new InputStreamReader(in), collectionType);

    }

    public Map<String, Integer> getLast24HoursOccurences(String systemUid, String data)
            throws IOException, AirVantageException {

        URL url = new URL(buildEndpoint("/systems/" + systemUid + "/data/" + data + "/aggregated")
                + "&fn=occ&interval=24hour&from=" + (System.currentTimeMillis() - 23 * 60 * 60 * 1000) + "&to="
                + (System.currentTimeMillis() + 1 * 60 * 60 * 1000));

        InputStream in = this.get(url);

        Map<String, Integer> values = new HashMap<String, Integer>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            in.close();
            JSONArray json = new JSONArray(sb.toString());
            JSONObject jsonValues = json.getJSONObject(0).getJSONObject("value");
            for (int i = 0; i < jsonValues.names().length(); i++) {
                values.put(jsonValues.names().getString(i).trim(), jsonValues.getInt(jsonValues.names().getString(i)));
            }
        } catch (JSONException e) {
            Log.e(AirVantageClient.class.getName(), "Unable to parse json", e);
            Crashlytics.logException(e);
        }

        return values;

    }

    public net.airvantage.model.AvSystem getSystemByUid(String uid) throws IOException, AirVantageException {
        URL url = new URL(
                buildEndpoint("/systems") + "&fields=uid,name,commStatus,lastCommDate,data,applications&uid=" + uid);
        InputStream in = get(url);
        List<net.airvantage.model.AvSystem> items = gson.fromJson(new InputStreamReader(in), SystemsList.class).items;
        if (items.size() > 0) {
            return items.get(0);
        } else {
            return null;
        }
    }

    public List<net.airvantage.model.AvSystem> getSystems() throws IOException, AirVantageException {
        return getSystemsBySerialNumber(null);
    }

    public List<net.airvantage.model.AvSystem> getSystemsBySerialNumber(String serialNumber)
            throws IOException, AirVantageException {
        String urlString = buildEndpoint("/systems")
                + "&fields=uid,name,commStatus,lastCommDate,data,applications,gateway";
        if (serialNumber != null) {
            urlString += "&gateway=serialNumber:" + serialNumber;
        }
        URL url = new URL(urlString);
        InputStream in = this.get(url);
        return gson.fromJson(new InputStreamReader(in), SystemsList.class).items;
    }

    public net.airvantage.model.AvSystem createSystem(net.airvantage.model.AvSystem system)
            throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/systems"));
        InputStream in = post(url, system);
        return gson.fromJson(new InputStreamReader(in), net.airvantage.model.AvSystem.class);
    }

    @Override
    public void updateSystem(AvSystem system) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/systems/") + system.uid);
        put(url, system);
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

    @Override
    public AlertRule getAlertRuleByName(final String name) throws IOException, AirVantageException {

        String str = Uri.parse(buildEndpoint("/alerts/rules")).buildUpon()
                .appendQueryParameter("access_token", access_token).appendQueryParameter("fields", "uid,name")
                .appendQueryParameter("name", name).build().toString();

        URL url = new URL(str);

        String s = url.toString();

        System.out.println(s);

        InputStream in = get(url);
        AlertRuleList rules = gson.fromJson(new InputStreamReader(in), AlertRuleList.class);
        return Utils.firstWhere(rules.items, AlertRule.isNamed(name));
    }

    @Override
    public AlertRule createAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/alerts/rules"));
        InputStream in = post(url, alertRule);
        return gson.fromJson(new InputStreamReader(in), AlertRule.class);
    }

    @Override
    public AlertRule updateAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint("/alerts/rules/" + alertRule.uid));
        put(url, alertRule);
        InputStream in = put(url, alertRule);
        return gson.fromJson(new InputStreamReader(in), AlertRule.class);
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
