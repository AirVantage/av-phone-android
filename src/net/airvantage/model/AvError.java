package net.airvantage.model;

import java.util.Arrays;
import java.util.List;

public class AvError {

    private static final Object SYSTEM_EXISTS = "system.not.unique.identifiers";
    private static final Object GATEWAY_EXISTS = "gateway.not.unique.identifiers";

    private static final Object APPLICATION_TYPE_EXISTS = "application.type.already.used";
    private static final String ALERT_RULES_TOO_MANY = "alert.rule.too.many";
    
    public String error;
    public List<String> errorParameters;

    public AvError(String error) {
        this.error = error;
        this.errorParameters = Arrays.asList();
    }

    public AvError(String error, List<String> errorParameters) {
        super();
        this.error = error;
        this.errorParameters = errorParameters;
    }

    public boolean systemAlreadyExists() {
        return (SYSTEM_EXISTS.equals(error) || GATEWAY_EXISTS.equals(error));
    }

    public boolean applicationAlreadyUsed() {
        return (APPLICATION_TYPE_EXISTS.equals(error));
    }
    
    public boolean tooManyAlerRules() {
        return (ALERT_RULES_TOO_MANY.equals(error));
    }
}
