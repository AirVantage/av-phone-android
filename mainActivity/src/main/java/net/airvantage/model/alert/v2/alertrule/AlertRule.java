package net.airvantage.model.alert.v2.alertrule;


import net.airvantage.utils.Predicate;

import java.util.List;
import java.util.Map;

public class AlertRule {

    public String id;
    public boolean active;
    public List<String> emails;

    public String companyId;
    public String message;
    public String name;
    public String notifMode;
    public String targetType;

    public List<Condition> conditions;

    public Map<String, Object> metadata;


    public static Predicate<AlertRule> isNamed(final String name) {
        return new Predicate<AlertRule>() {
            @Override
            public boolean matches(AlertRule item) {
                return name.equals(item.name);
            }
        };
    }
}