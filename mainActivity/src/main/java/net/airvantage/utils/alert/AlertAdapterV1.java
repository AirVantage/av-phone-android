package net.airvantage.utils.alert;

import android.net.Uri;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.model.alert.v1.AlertRuleList;
import net.airvantage.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;


public class AlertAdapterV1 extends DefaultAlertAdapter {

  private static String API_PATH ="/alerts/rules";

    public AlertAdapterV1(String server, String accessToken) {
        super(server, accessToken);
    }

    @Override
    public String getPrefix() {
        return "/api/v1";
    }

    @Override
    public AlertRule getAlertRuleByName(final String name) throws IOException, AirVantageException {

        String str = Uri.parse(buildEndpoint(API_PATH))
                .buildUpon()
                .appendQueryParameter("access_token", access_token)
                .appendQueryParameter("fields", "uid,name")
                .appendQueryParameter("name", name)
                .build()
                .toString();

        URL url = new URL(str);
        InputStream in = get(url);
        AlertRuleList rules = gson.fromJson(new InputStreamReader(in), AlertRuleList.class);
        return Utils.firstWhere(rules.items, AlertRule.isNamed(name));
    }

    @Override
    public AlertRule createAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint(API_PATH));
        InputStream in = post(url, alertRule);
        return gson.fromJson(new InputStreamReader(in), AlertRule.class);
    }

    @Override
    public AlertRule updateAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        URL url = new URL(buildEndpoint(API_PATH + alertRule.uid));
        put(url, alertRule);
        InputStream in = put(url, alertRule);
        return gson.fromJson(new InputStreamReader(in), AlertRule.class);
    }
}
