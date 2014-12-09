package net.airvantage.utils;

public class AvPhonePrefs {
    
    public String serverHost;
    public String clientId;
    
    public String password;
    public String period;
    
    // TODO(pht) use enum for server
    public boolean usesNA;
    public boolean usesEU;
    
    public boolean checkCredentials() {
        return !(password == null || password.isEmpty() || serverHost == null || serverHost.isEmpty());
    }

    public boolean usesNA() {
        return usesNA;
    }
    
    public boolean usesEU() {
        return usesEU;
    }

    public boolean usesCustomServer() {
        return (!usesNA && !usesEU);
    }
    
}