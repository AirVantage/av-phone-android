package net.airvantage.utils.alert;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class AlertAdapterFactory extends AsyncTask<Void, Void, DefaultAlertAdapter> {

    private static final String ALERT_V1_API_PREFIX = "/api/v1/alerts/rules?size=0&access_token=";
    private static final String ALERT_V2_API_PREFIX = "/api/v2/alertstates?access_token=";
    private final IAlertAdapterFactoryListener listener;

    private final String server;
    private final String accessToken;
    private final OkHttpClient client;

    public AlertAdapterFactory(String server, String token, IAlertAdapterFactoryListener adapterListener) {
        this.accessToken = token;
        this.client = new OkHttpClient();
        this.listener = adapterListener;
        this.server = server;
        this.execute();
    }

    @Override
    protected DefaultAlertAdapter doInBackground(Void... params) {

        // Mapping of _Alert APIs_ -> Functions handling them
        HashMap<String, DefaultAlertAdapter> urls = new HashMap<String, DefaultAlertAdapter>();
        urls.put(ALERT_V1_API_PREFIX, new AlertAdapterV1(server, accessToken));
        urls.put(ALERT_V2_API_PREFIX, new AlertAdapterV2(server, accessToken));

        // We using first available
        for (HashMap.Entry<String, DefaultAlertAdapter> entry : urls.entrySet()) {
            try {

                String urlString = "https://" + server + entry.getKey() + accessToken;
                URL url = new URL(urlString);
                HttpURLConnection connection = client.open(url);
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.d(this.getClass().getName(), "Using Alerts from " + urlString);
                    return entry.getValue();
                }

            } catch (MalformedURLException e) {
                Log.e(this.getClass().getName(), "Bad Url generated for " + entry.getKey(), e);
            } catch (IOException e) {
                Log.e(this.getClass().getName(), "Connection problem", e);
            }
        }

        // Neither is available?
        // This adapter provides error messages at each call
        return new DefaultAlertAdapter(server, accessToken);
    }

    @Override
    protected void onPostExecute(DefaultAlertAdapter adapter) {
        this.listener.alertAdapterAvailable(adapter);
    }
}
