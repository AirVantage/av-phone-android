package net.airvantage.utils

import java.util.Date

import android.net.Uri
import android.util.Log

import com.sierrawireless.avphone.auth.Authentication

class AuthenticationUrlParser {

    private val TAG = this::class.java.name

    fun parseUrl(url: String, parsingDate: Date): Authentication? {

        var auth: Authentication? = null

        if (url.startsWith("oauth")) {
            Log.i(TAG, "Callback URL: " + url)
            // Example
            // oauth://airvantage#access_token=430ad00a-2737-4673-bfa4-48c6710d748f&token_type=bearer&expires_in=86399&scope=
            val uri = Uri.parse(url)

            if (uri.host == "airvantage") {
                auth = Authentication()

                val fragment = uri.fragment
                if (fragment != null) {

                    val params = fragment.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (param in params) {
                        val kv = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (kv.size == 2) {
                            val key = kv[0]
                            val value = kv[1]

                            if ("access_token" == key) {
                                auth.accessToken = value
                            } else if ("expires_in" == key) {
                                val expiresInSeconds = Integer.parseInt(value)
                                val expirationDate = Date(parsingDate.time + expiresInSeconds * 1000)
                                auth.expirationDate = expirationDate
                            }
                        }
                    }
                }
            }
        }
        return auth
    }
}
