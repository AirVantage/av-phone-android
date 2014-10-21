package net.airvantage.utils;

public class AvPhonePrefs {
	public String serverHost;
	public String password;
	public String period;
	
	public boolean checkCredentials() {
		 return !(password == null || password.isEmpty() || serverHost == null || serverHost.isEmpty());
	}
	
}
