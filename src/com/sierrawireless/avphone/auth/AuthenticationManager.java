package com.sierrawireless.avphone.auth;

public interface AuthenticationManager {
    public void onAuthentication(Authentication auth);
    public void forgetAuthentication();
    public boolean isLogged();
    public Authentication getAuthentication();
}
