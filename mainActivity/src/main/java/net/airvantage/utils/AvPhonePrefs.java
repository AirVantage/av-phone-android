package net.airvantage.utils;

public class AvPhonePrefs {

    public String serverHost;
    public String clientId;

    public String password;
    public String period;

    public PreferenceUtils.Server usesServer;

    public boolean checkCredentials() {
        return !(password == null || password.isEmpty() || serverHost == null || serverHost.isEmpty());
    }

    public boolean usesNA() {
        return usesServer == PreferenceUtils.Server.NA;
    }

    public boolean usesEU() {
        return usesServer == PreferenceUtils.Server.EU;
    }

    public boolean usesCustomServer() {
        return usesServer == PreferenceUtils.Server.CUSTOM;
    }

}
