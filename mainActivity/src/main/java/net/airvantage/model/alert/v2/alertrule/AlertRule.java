package net.airvantage.model.alert.v2.alertrule;


import android.util.Log;

import net.airvantage.utils.Predicate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlertRule {

    public String id;
    public boolean active;

    public String message;
    public String name;
    public String targetType;

    public List<Condition> conditions;
    public HashMap<String, Object> metadata;
    private static final String TAG = "AlertRule";


    public static Predicate<AlertRule> isNamed(final String name) {
        return new Predicate<AlertRule>() {
            @Override
            public boolean matches(AlertRule item) {
                Log.d(TAG, "matches: " + name + " and " + item.name);
                boolean match = name.equals(item.name);
                Log.d(TAG, "matches: result " +match);
                return match;
            }
        };
    }
}
