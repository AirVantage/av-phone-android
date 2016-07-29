package com.sierrawireless.avphone;

import java.util.Date;

import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.AuthenticationUrlParser;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.sierrawireless.avphone.auth.Authentication;

public class AuthorizationActivity extends Activity {

    private static final String LOGTAG = AuthorizationActivity.class.getName();

    public static final String AUTHENTICATION_TOKEN = "token";
    public static final String AUTHENTICATION_EXPIRATION_DATE = "expirationDate";

    public static final String AUTHENTICATION_LISTENER = "listener";

    public static final int REQUEST_AUTHORIZATION = 1;


    private WebView webview;

    private AuthenticationUrlParser authUrlParser = new AuthenticationUrlParser();

    private Drawable enabledButtonBg;
    private Drawable disabledButtonBg;

    private Button btnCustom;
    private Button btnEu;
    private Button btnNa;

    private boolean isEu = true;
    private PreferenceUtils.Server currentServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        btnCustom = (Button) this.findViewById(R.id.auth_btn_custom);
        btnEu = (Button) this.findViewById(R.id.auth_btn_eu);
        btnNa = (Button) this.findViewById(R.id.auth_btn_na);

        if (!PreferenceUtils.isCustomDefined(this)) {
            btnCustom.setVisibility(Button.GONE);
        }

        if (disabledButtonBg == null) {
            disabledButtonBg = btnEu.getBackground();
        } else {
            Log.w(LOGTAG, "disabledButtonBg has already been loaded, this can cause bug...");
        }
        enabledButtonBg = getResources().getDrawable(R.drawable.apptheme_switch_thumb_activated_holo_light);

        btnNa.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentServer == PreferenceUtils.Server.NA) {
                    return;
                }

                currentServer = PreferenceUtils.Server.NA;
                setButtonEnabled(btnNa);
                setButtonDisabled(btnCustom);
                setButtonDisabled(btnEu);
                PreferenceUtils.setServer(PreferenceUtils.Server.NA, AuthorizationActivity.this);
                openAuthorizationPage();
            }
        });

        btnEu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentServer == PreferenceUtils.Server.EU) {
                    return;
                }

                currentServer = PreferenceUtils.Server.EU;
                setButtonEnabled(btnEu);
                setButtonDisabled(btnCustom);
                setButtonDisabled(btnNa);
                PreferenceUtils.setServer(PreferenceUtils.Server.EU, AuthorizationActivity.this);
                openAuthorizationPage();
            }
        });

        btnCustom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentServer == PreferenceUtils.Server.CUSTOM) {
                    return;
                }

                setButtonEnabled(btnCustom);
                setButtonDisabled(btnEu);
                setButtonDisabled(btnNa);
                PreferenceUtils.setServer(PreferenceUtils.Server.CUSTOM, AuthorizationActivity.this);
                openAuthorizationPage();
            }
        });

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(this);
        if (avPhonePrefs.usesEU()) {
            this.isEu = true;
            setButtonEnabled(btnEu);
            setButtonDisabled(btnNa);
        } else {
            this.isEu = false;
            setButtonEnabled(btnNa);
            setButtonDisabled(btnEu);
        }

        openAuthorizationPage();
    }

    private void setButtonEnabled(Button btn) {
        btn.setBackground(enabledButtonBg);
        btn.setTextColor(getResources().getColor(R.color.white));
    }

    private void setButtonDisabled(Button btn) {
        btn.setBackground(disabledButtonBg);
        btn.setTextColor(getResources().getColor(R.color.textcolor));
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void openAuthorizationPage() {

        AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(this);

        final String serverHost = avPhonePrefs.serverHost;
        final String clientId = avPhonePrefs.clientId;

        webview = (WebView) findViewById(R.id.authorization_webview);
        webview.getSettings().setJavaScriptEnabled(true);
        // attach WebViewClient to intercept the callback url
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                final Authentication auth = authUrlParser.parseUrl(url, new Date());

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

        // Example :
        // https://na.airvantage.net/api/oauth/authorize?client_id=54d4faa5343d49fba03f2a2ec1f210b9&response_type=token&redirect_uri=oauth://airvantage
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
