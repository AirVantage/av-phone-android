package com.sierrawireless.avphone.auth

interface AuthenticationManager {
    val isLogged: Boolean
    var authentication: Authentication?
    fun onAuthentication(auth: Authentication)
    fun forgetAuthentication()
}
