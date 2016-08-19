package com.sierrawireless.avphone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.sierrawireless.avphone.auth.Authentication;

import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.AuthenticationUrlParser;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import java.util.Date;

public class AuthorizationActivity extends Activity {

    private static final String LOGTAG = AuthorizationActivity.class.getName();

    public static final String AUTHENTICATION_TOKEN = "token";
    public static final String AUTHENTICATION_EXPIRATION_DATE = "expirationDate";

    public static final String AUTHENTICATION_LISTENER = "listener";

    public static final int REQUEST_AUTHORIZATION = 1;


    private WebView webview;

    private AuthenticationUrlParser authUrlParser = new AuthenticationUrlParser();

    private Button btnCustom;
    private Button btnEu;
    private Button btnNa;

    private PreferenceUtils.Server currentServer;

    private final class OnHostClickListener implements OnClickListener {

        private final PreferenceUtils.Server server;

        public OnHostClickListener(final PreferenceUtils.Server targetServer) {
            server = targetServer;
        }

        @Override
        public void onClick(final View v) {
            if (currentServer != server) {
                currentServer = server;
                PreferenceUtils.setServer(server, AuthorizationActivity.this);
                openAuthorizationPage();
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        btnNa = (RadioButton) this.findViewById(R.id.auth_btn_na);
        btnEu = (RadioButton) this.findViewById(R.id.auth_btn_eu);
        btnCustom = (RadioButton) this.findViewById(R.id.auth_btn_custom);

        btnNa.setOnClickListener(new OnHostClickListener(PreferenceUtils.Server.NA));
        btnEu.setOnClickListener(new OnHostClickListener(PreferenceUtils.Server.EU));
        btnCustom.setOnClickListener(new OnHostClickListener(PreferenceUtils.Server.CUSTOM));

        if (PreferenceUtils.isCustomDefined(this)) {
            final RadioGroup parentRadioGroup = (RadioGroup) btnCustom.getParent();
            parentRadioGroup.check(btnCustom.getId());
        } else {
            btnCustom.setVisibility(Button.GONE);
        }

        openAuthorizationPage();
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
