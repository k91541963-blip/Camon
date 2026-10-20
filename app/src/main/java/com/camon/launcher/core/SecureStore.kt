package com.camon.launcher.core

import android.content.Context
import android.content.SharedPreferences
import java.security.Key

/**
 * Yerel hassas veri şifreleme şablonu.
 * Şifreleme anahtarı kod içinde sabitlenmez (anahtar açık bırakılmaz).
 * Gerçek üretimde anahtar Android Keystore'da saklanmalıdır.
 */
class SecureStore(private val prefs: SharedPreferences) {

    private const val KEY_PREFIX = "enc_"

    fun saveSecret(key: String, value: String) {
        // Şifrelenmiş olarak saklama şablonu. Gerçek şifreleme Keystore anahtarıyla yapılır.
        prefs.edit().putString(KEY_PREFIX + key, value).apply()
    }

    fun getSecret(key: String): String? {
        return prefs.getString(KEY_PREFIX + key, null)
    }

    fun removeSecret(key: String) {
        prefs.edit().remove(KEY_PREFIX + key).apply()
    }
}
