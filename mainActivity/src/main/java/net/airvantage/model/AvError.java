package net.airvantage.model;

import java.util.Collections;
import java.util.List;

public class AvError {

    private static final String SYSTEM_EXISTS = "system.not.unique.identifiers";
    private static final String GATEWAY_EXISTS = "gateway.not.unique.identifiers";
    private static final String GATEWAY_ASSIGNED = "gateway.assigned";
    public static final String FORBIDDEN = "forbidden";
    private static final String APPLICATION_TYPE_EXISTS = "application.type.already.used";
    private static final String ALERT_RULES_TOO_MANY = "alert.rule.too.many";
    public static final String MISSING_RIGHTS = "missing.rights";
    
    public String error;
    public List<String> errorParameters;

    public AvError(String error) {
        this.error = error;
        this.errorParameters = Collections.emptyList();
    }

    public AvError(String error, List<String> errorParameters) {
        super();
        this.error = error;
        this.errorParameters = errorParameters;
    }

    public boolean systemAlreadyExists() {
        return (SYSTEM_EXISTS.equals(error) || GATEWAY_EXISTS.equals(error));
    }

    public boolean gatewayAlreadyExists() {
        return (GATEWAY_ASSIGNED.equals(error));
    }

    public boolean applicationAlreadyUsed() {
        return (APPLICATION_TYPE_EXISTS.equals(error));
    }

    public boolean tooManyAlerRules() {
        return (ALERT_RULES_TOO_MANY.equals(error));
    }

    public boolean forbidden() {
        return FORBIDDEN.equals(error);
    }

    public boolean cantCreateApplication() {
        return isForbiddenAction("POST", "application");
    }

    public boolean cantCreateSystem() {
        return isForbiddenAction("POST", "system");
    }

    public boolean cantCreateAlertRule() {
        return isForbiddenAction("POST", "alerts/rules");
    }

    public boolean cantUpdateApplication() {
        return isForbiddenAction("PUT", "application");
    }

    public boolean cantUpdateSystem() {
        return isForbiddenAction("PUT", "system");
    }

    private boolean isForbiddenAction(String method, String entity) {
        if (forbidden()) {
            String requestMethod = errorParameters.get(0);
            String requestUrl = errorParameters.get(1);
            return method.equalsIgnoreCase(requestMethod) && requestUrl.contains(entity);
        } else {
            return false;
        }
    }

    public boolean missingRights() {
        return MISSING_RIGHTS.equals(error);
    }

}
