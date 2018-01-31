package com.sierrawireless.avphone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.sierrawireless.avphone.auth.Authentication
import net.airvantage.utils.AirVantageClient
import net.airvantage.utils.AuthenticationUrlParser
import net.airvantage.utils.PreferenceUtils
import java.util.*

class AuthorizationActivity : Activity() {

    private lateinit var btnNa: RadioButton
    private lateinit var btnEu: RadioButton
    private lateinit var btnCustom: RadioButton

    private val authUrlParser = AuthenticationUrlParser()

    private var currentServer: PreferenceUtils.Server? = null

    private inner class OnHostClickListener (private val server: PreferenceUtils.Server) : OnClickListener {

        override fun onClick(v: View) {
            if (currentServer != server) {
                currentServer = server
                PreferenceUtils.setServer(server, this@AuthorizationActivity)
                openAuthorizationPage()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authorization)

        btnNa = this.findViewById(R.id.auth_btn_na)
        btnEu = this.findViewById(R.id.auth_btn_eu)
        btnCustom = this.findViewById(R.id.auth_btn_custom)

        btnNa.setOnClickListener(OnHostClickListener(PreferenceUtils.Server.NA))
        btnEu.setOnClickListener(OnHostClickListener(PreferenceUtils.Server.EU))
        btnCustom.setOnClickListener(OnHostClickListener(PreferenceUtils.Server.CUSTOM))

        if (PreferenceUtils.isCustomDefined(this)) {
            val parentRadioGroup = btnCustom.parent as RadioGroup
            parentRadioGroup.check(btnCustom.id)
        } else {
            btnCustom.visibility = Button.GONE
        }

        openAuthorizationPage()
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun openAuthorizationPage() {

        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(this)

        when {
            avPhonePrefs.usesNA() -> {
                btnNa.isChecked = true
                btnCustom.isChecked = false
                btnEu.isChecked = false
            }
            avPhonePrefs.usesEU() -> {
                btnNa.isChecked = false
                btnCustom.isChecked = false
                btnEu.isChecked = true
            }
            else -> {
                btnNa.isChecked = false
                btnCustom.isChecked = true
                btnEu.isChecked = false
            }
        }

        val serverHost = avPhonePrefs.serverHost
        val clientId = avPhonePrefs.clientId

        val webview = findViewById<WebView>(R.id.authorization_webview)
        webview.settings.javaScriptEnabled = true
        // attach WebViewClient to intercept the callback url
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {

                val auth = authUrlParser.parseUrl(request.url.toString(), Date())

                if (auth != null) {
                    Log.d(AuthorizationActivity::class.java.name, "Access token: " + auth.accessToken!!)
                    Log.d(AuthorizationActivity::class.java.name, "Expiration date : " + auth.expirationDate!!)

                    sendAuthentication(auth)

                }

                return super.shouldOverrideUrlLoading(view, request)
            }

        }
        val authUrl = AirVantageClient.buildImplicitFlowURL(serverHost, clientId)
        Log.d(AuthorizationActivity::class.java.name, "Auth URL: " + authUrl)

        // The 'authorize' page from AirVantage will store a cookie ;
        // if this cookie is passed between calls, the 'authorize' page
        // will not be displayed at all.
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)


        // Example :
        // https://na.airvantage.net/api/oauth/authorize?client_id=54d4faa5343d49fba03f2a2ec1f210b9&response_type=token&redirect_uri=oauth://airvantage
        webview.loadUrl(authUrl)
    }

    private fun sendAuthentication(auth: Authentication?) {

        val resultIntent = Intent()

        resultIntent.putExtra(AUTHENTICATION_TOKEN, auth!!.accessToken)
        resultIntent.putExtra(AUTHENTICATION_EXPIRATION_DATE, auth.expirationDate!!.time)

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {

        const val AUTHENTICATION_TOKEN = "token"
        const val AUTHENTICATION_EXPIRATION_DATE = "expirationDate"

        const val REQUEST_AUTHORIZATION = 1
    }

}
