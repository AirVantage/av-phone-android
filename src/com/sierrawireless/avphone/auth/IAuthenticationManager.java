package com.sierrawireless.avphone.auth;

import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
import android.support.v4.app.Fragment;

public interface IAuthenticationManager {

    public Authentication getAuthentication(PreferenceUtils prefUtils);
    
    public void authenticate(PreferenceUtils prefUtils, Fragment fragment, AuthenticationListener listener);
    
    public Authentication activityResultAsAuthentication(int requestCode, int resultCode, Intent data);
    
    public void saveAuthentication(PreferenceUtils prefUtils, Authentication auth);
    
    public void forgetAuthentication(PreferenceUtils prefUtils);
}
