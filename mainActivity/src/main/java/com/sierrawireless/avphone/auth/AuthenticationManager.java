package com.sierrawireless.avphone.auth;

public interface AuthenticationManager {
    void onAuthentication(Authentication auth);
    void forgetAuthentication();
    boolean isLogged();
    Authentication getAuthentication();
}
