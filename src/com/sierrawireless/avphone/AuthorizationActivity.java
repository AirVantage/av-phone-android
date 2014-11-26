package com.sierrawireless.avphone;

import java.util.Date;

import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.AuthenticationUrlParser;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sierrawireless.avphone.auth.Authentication;

public class AuthorizationActivity extends Activity {

    public static final String AUTHENTICATION_TOKEN = "token";
    public static final String AUTHENTICATION_EXPIRATION_DATE = "expirationDate";

    public static final String AUTHENTICATION_LISTENER = "listener";

    public static final int REQUEST_AUTHORIZATION = 1;

    private WebView webview;

    private AuthenticationUrlParser authUrlParser = new AuthenticationUrlParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        openAuthorizationPage();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openAuthorizationPage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String serverHost = prefs.getString(this.getString(R.string.pref_server_key), null);
        String clientId = prefs.getString(this.getString(R.string.pref_client_id_key), null);

        webview = (WebView) findViewById(R.id.authorization_webview);
        webview.getSettings().setJavaScriptEnabled(true);
        // attach WebViewClient to intercept the callback url
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Authentication auth = authUrlParser.parseUrl(url, new Date());

                if (auth != null) {
                    Log.d(AuthorizationActivity.class.getName(), "Access token: " + auth.getAccessToken());
                    Log.d(AuthorizationActivity.class.getName(), "Expiration date : " + auth.getExpirationDate());
                    sendAuthentication(auth);
                }

                return super.shouldOverrideUrlLoading(view, url);
            }

        });
        String authUrl = AirVantageClient.buildImplicitFlowURL(serverHost, clientId);
        Log.d(AuthorizationActivity.class.getName(), "Auth URL: " + authUrl);
        
        // The 'authorize' page from AirVantage will store a cookie ; 
        // if this cookie is passed between calls, the 'authorize' page
        // will not be displayed at all.
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        
        // Example : https://na.airvantage.net/api/oauth/authorize?client_id=54d4faa5343d49fba03f2a2ec1f210b9&response_type=token&redirect_uri=oauth://airvantage
        webview.loadUrl(authUrl);
    }

    private void sendAuthentication(Authentication auth) {

        Intent resultIntent = new Intent();

        resultIntent.putExtra(AUTHENTICATION_TOKEN, auth.getAccessToken());
        resultIntent.putExtra(AUTHENTICATION_EXPIRATION_DATE, auth.getExpirationDate().getTime());

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
