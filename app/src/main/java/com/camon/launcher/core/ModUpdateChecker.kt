package com.camon.launcher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mod güncelleme kontrolü.
 * Gerçek bir Modrinth/CurseForge API entegrasyonu burada yer alabilir.
 * Şu anlık şablon bir yapıdadır: herhangi bir API anahtarı saklanmaz.
 */
object ModUpdateChecker {

    data class UpdateInfo(
        val modName: String,
        val currentVersion: String,
        val latestVersion: String,
        val updateUrl: String? = null
    )

    fun checkForUpdates(mods: List<ModItem>): List<UpdateInfo> = mods.mapNotNull { mod ->
        val latest = checkLatestVersion(mod)
        if (latest != mod.version) {
            UpdateInfo(mod.name, mod.version, latest)
        } else null
    }

    private fun checkLatestVersion(mod: ModItem): String {
        // Şablon: gerçek entegrasyon burada API'ye istek atar.
        // API anahtarı kod içinde saklanmaz (anahtar açık bırakılmaz).
        return mod.version
    }

    fun runBackgroundCheck(mods: List<ModItem>, callback: (List<UpdateInfo>) -> Unit) {
        // Dispatchers.Default ile arka planda çalıştırılabilir.
        val updates = checkForUpdates(mods)
        callback(updates)
    }
}
