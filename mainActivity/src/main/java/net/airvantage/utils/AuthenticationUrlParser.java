package net.airvantage.utils;

import java.util.Date;

import android.net.Uri;
import android.util.Log;

import com.sierrawireless.avphone.auth.Authentication;

public class AuthenticationUrlParser {

    public Authentication parseUrl(String url, Date parsingDate) {

        Authentication auth = null;

        if (url.startsWith("oauth")) {
            Log.d(AuthenticationUrlParser.class.getName(), "Callback URL: " + url);
            // Example
            // oauth://airvantage#access_token=430ad00a-2737-4673-bfa4-48c6710d748f&token_type=bearer&expires_in=86399&scope=
            Uri uri = Uri.parse(url);

            if (uri.getHost().equals("airvantage")) {
                auth = new Authentication();

                String fragment = uri.getFragment();
                if (fragment != null) {

                    String[] params = fragment.split("&");
                    for (String param : params) {
                        String[] kv = param.split("=");
                        if (kv != null && kv.length == 2) {
                            String key = kv[0];
                            String value = kv[1];

                            if ("access_token".equals(key)) {
                                auth.setAccessToken(value);
                            } else if ("expires_in".equals(key)) {
                                int expiresInSeconds = Integer.parseInt(value);
                                Date expirationDate = new Date(parsingDate.getTime() + expiresInSeconds * 1000);
                                auth.setExpirationDate(expirationDate);
                            }
                        }
                    }
                }
            }

        }

        return auth;
    }

}