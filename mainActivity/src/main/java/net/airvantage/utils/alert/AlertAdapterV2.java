package net.airvantage.utils.alert;

import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonIOException;
import com.sierrawireless.avphone.model.AvPhoneData;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v2.AlertRuleList;
import net.airvantage.model.alert.v2.alertrule.AlertRule;
import net.airvantage.model.alert.v2.alertrule.AttributeId;
import net.airvantage.model.alert.v2.alertrule.Condition;
import net.airvantage.model.alert.v2.alertrule.Operand;
import net.airvantage.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class AlertAdapterV2 extends DefaultAlertAdapter {

    private static String LOG_TAG = AlertAdapterV2.class.getName();

    private static String API_PATH = "alertrules";
    private URL alertRuleUrl = null;

    public AlertAdapterV2(String server, String accessToken) {
        super(server, accessToken);
    }

    @Override
    public String getPrefix() {
        return "/api/v2/";
    }

    @Override
    public net.airvantage.model.alert.v1.AlertRule getAlertRuleByName(final String name)
            throws IOException, AirVantageException {
        try {
            InputStream in = get(alertRuleUrl());
            AlertRuleList rules = gson.fromJson(new InputStreamReader(in), AlertRuleList.class);
            AlertRule alertRuleV2 = Utils.firstWhere(rules, AlertRule.isNamed(name));
            if (alertRuleV2 != null) {
                convert(alertRuleV2);
            }
        } catch (final JsonIOException e) {
            Log.e(LOG_TAG, "Unable to read Alert Rules", e);
        }
        return null;
    }

    @Override
    public net.airvantage.model.alert.v1.AlertRule createAlertRule(net.airvantage.model.alert.v1.AlertRule alertRule)
            throws IOException, AirVantageException {
        try {
            AlertRule alertRuleV2 = new AlertRule();
            alertRuleV2.targetType = "SYSTEM";
            alertRuleV2.name = alertRule.name;
            alertRuleV2.message = "Alarm is ON";
            alertRuleV2.conditions = new ArrayList<Condition>();
            for (net.airvantage.model.alert.v1.Condition condition : alertRule.conditions) {
                alertRuleV2.conditions.add(convert(condition));
            }
            InputStream in = post(alertRuleUrl(), alertRuleV2);
            alertRuleV2 = gson.fromJson(new InputStreamReader(in), AlertRule.class);
            if (alertRuleV2 != null) {
                return convert(alertRuleV2);
            }
        } catch (final JsonIOException e) {
            Log.e(LOG_TAG, "Unable to create Alert Rule", e);
        }
        return null;
    }

    private static Condition convert(net.airvantage.model.alert.v1.Condition condition) {

        final Condition conditionV2 = new Condition();
        conditionV2.operator = condition.operator;

        final Operand leftOperand = new Operand();
        leftOperand.attributeId = new AttributeId();
        leftOperand.attributeId.name = "communication.data.value";

        final Operand rightOperand = new Operand();
        rightOperand.valueStr = condition.value;

        conditionV2.operands = new ArrayList<Operand>(Arrays.asList(leftOperand, rightOperand));
        return conditionV2;
    }

    @Override
    public net.airvantage.model.alert.v1.AlertRule updateAlertRule(
            net.airvantage.model.alert.v1.AlertRule alertRule)
            throws IOException, AirVantageException {
        try {
            InputStream in = put(alertRuleUrl(), alertRule);
            AlertRule alertRuleV2 = gson.fromJson(new InputStreamReader(in), AlertRule.class);
            if (alertRuleV2 != null) {
                return convert(alertRuleV2);
            }
        } catch (final JsonIOException e) {
            Log.e(LOG_TAG, "Unable to update Alert Rule", e);
        }
        return null;
    }

    private URL alertRuleUrl() throws IOException {

        if (alertRuleUrl != null)
            return alertRuleUrl;

        String urlString = Uri.parse(buildEndpoint(API_PATH)).toString();
        try {
            alertRuleUrl = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(this.getClass().getName(), "Sure of URL?", e);
            throw e;
        }

        return alertRuleUrl;
    }

    private static net.airvantage.model.alert.v1.AlertRule convert(AlertRule alertRule) {
        net.airvantage.model.alert.v1.AlertRule alertRuleV1;
        alertRuleV1 = new net.airvantage.model.alert.v1.AlertRule();

        // Assign similar fields
        alertRuleV1.uid = alertRule.id;
        alertRuleV1.active = alertRule.active;
        alertRuleV1.name = alertRule.name;
        // alertRuleV1.metadata = alertRule.metadata;

        // Only event created in AV Phone
        alertRuleV1.eventType = "event.system.incoming.communication";

        // Translating conditions
        alertRuleV1.conditions = new ArrayList<net.airvantage.model.alert.v1.Condition>();
        for (final Condition condition : alertRule.conditions) {
            alertRuleV1.conditions.add(convert(condition));
        }
        return alertRuleV1;
    }

    private static net.airvantage.model.alert.v1.Condition convert(Condition condition) {
        net.airvantage.model.alert.v1.Condition conditionV1;
        conditionV1 = new net.airvantage.model.alert.v1.Condition();
        conditionV1.eventProperty = "communication.data.value";
        conditionV1.eventPropertyKey = AvPhoneData.ALARM;
        conditionV1.operator = condition.operator;

        // Finding values
        Operand operand = Utils.first(condition.operands);
        List<Serializable> values = new ArrayList<Serializable>();
        values.add(operand.valueStr);
        values.add(operand.valueNum);
        values.add(Utils.first(operand.valuesStr));

        // Trimming nulls
        values.removeAll(Collections.singleton(null));
        values.removeAll(Collections.singleton(""));

        // Pick first one
        conditionV1.value = values.isEmpty() ? "" : Utils.first(values).toString();
        return conditionV1;
    }
}
