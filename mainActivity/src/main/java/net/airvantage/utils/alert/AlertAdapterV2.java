package net.airvantage.utils.alert;

import android.net.Uri;
import android.util.Log;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.model.alert.v1.AlertRuleList;
import net.airvantage.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class AlertAdapterV2 extends DefaultAlertAdapter {

    private static String API_PATH = "alertrules";
    private URL alertRuleUrl = null;

    public AlertAdapterV2(String server, String accessToken) {
        super(server, accessToken);
    }

    @Override
    public String getPrefix() {
        return "/api/v2";
    }

    @Override
    public AlertRule getAlertRuleByName(final String name) throws IOException, AirVantageException {
        InputStream in = get(alertRuleUrl());
        AlertRuleList rules = gson.fromJson(new InputStreamReader(in), AlertRuleList.class);
        return Utils.firstWhere(rules.items, AlertRule.isNamed(name));
    }

    @Override
    public AlertRule createAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        InputStream in = post(alertRuleUrl(), alertRule);
        return gson.fromJson(new InputStreamReader(in), AlertRule.class);
    }

    @Override
    public AlertRule updateAlertRule(AlertRule alertRule) throws IOException, AirVantageException {
        InputStream in = put(alertRuleUrl(), alertRule);
        return gson.fromJson(new InputStreamReader(in), AlertRule.class);
    }

    private URL alertRuleUrl() throws IOException {

        if (alertRuleUrl != null)
            return alertRuleUrl;

        String urlString = Uri.parse(buildEndpoint(API_PATH)).buildUpon()
                .appendQueryParameter("access_token", access_token)
                .build()
                .toString();
        try {
            alertRuleUrl = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(null, null, e);
            throw e;
        }

        return alertRuleUrl;
    }
}
