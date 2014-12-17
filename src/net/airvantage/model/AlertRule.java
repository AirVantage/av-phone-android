package net.airvantage.model;

import java.util.List;
import java.util.Map;

import net.airvantage.utils.Predicate;

public class AlertRule {
    public String uid;
    public boolean active = true;
    public String name;
    public String eventType;
    public List<Condition> conditions;
    public Map<String, String> metadata;

    public static Predicate<AlertRule> isNamed(final String name) {
        return new Predicate<AlertRule>() {
            @Override
            public boolean matches(AlertRule item) {
                return name.equals(item.name);
            }
        };
    }
}
