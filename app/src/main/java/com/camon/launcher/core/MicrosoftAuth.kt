package com.camon.launcher.core

import android.content.Context

/**
 * Microsoft OAuth2 girişi şablonu.
 * Client ID gibi kimlik bilgileri kod içinde saklanmaz.
 * Gerçek akış: Microsoft kimlik doğrulama sunucusu üzerinden token alınır.
 */
class MicrosoftAuth(private val context: Context) {

    data class AuthResult(
        val success: Boolean,
        val accessToken: String? = null,
        val accountUuid: String? = null,
        val username: String? = null,
        val error: String? = null
    )

    fun startSignIn(): AuthResult {
        // Şablon: gerçek OAuth2 akışı burada WebView/Chrome Custom Tabs ile yapılır.
        // Client ID ve Client Secret kod içinde yoktur.
        return AuthResult(success = false, error = "OAuth2 akışı henüz entegre edilmedi.")
    }

    fun signOut() {
        // Token temizleme şablonu
    }
}
