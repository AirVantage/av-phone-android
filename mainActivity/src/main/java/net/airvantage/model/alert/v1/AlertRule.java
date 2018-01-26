package net.airvantage.model.alert.v1;

import net.airvantage.utils.Predicate;

import java.util.List;

public class AlertRule {
    public String uid;
    public boolean active = true;
    public String name;
    public String eventType;
    public List<Condition> conditions;

    public static Predicate<AlertRule> isNamed(final String name) {
        return new Predicate<AlertRule>() {
            @Override
            public boolean matches(AlertRule item) {
                return name.equals(item.name);
            }
        };
    }
}
