package com.sierrawireless.avphone.auth;

public interface AuthenticationListener {
    public void onAuthentication(Authentication auth);
    public void forgetAuthentication();
    public boolean isLogged();
    public Authentication getAuthentication();
}
