package net.airvantage.model;

import java.util.List;
import java.util.Map;

public class AlertRule {
    public boolean active = true;
    public String name;
    public String eventType;
    public List<Condition> conditions;
    public Map<String, String> metadata;
}
