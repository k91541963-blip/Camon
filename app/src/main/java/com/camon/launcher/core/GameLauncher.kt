package com.camon.launcher.core

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Zalith Launcher 2 varyantı - Minecraft Java işlemi başlatma ve yönetim.
 * Offline hesap desteği ile tamamen yerel kimlik doğrulama.
 */
class GameLauncher(private val context: Context) {

    data class LaunchResult(
        val success: Boolean,
        val processId: Int? = null,
        val error: String? = null
    )

    data class LaunchContext(
        val profile: Profile,
        val account: Account?,
        val config: LaunchConfig?,
        val gameDir: File,
        val javaMajor: Int,
        val ramMb: Int,
        val jvmArgs: String,
        val customArgs: String
    )

    fun launch(context: LaunchContext): LaunchResult {
        val javaCommand = buildJavaCommand(context)
        return try {
            val process = java.lang.Runtime.getRuntime().exec(javaCommand)
            LaunchResult(success = true, processId = process)
        } catch (e: Exception) {
            LaunchResult(success = false, error = e.message)
        }
    }

    fun launch(profile: Profile, gameDir: File): LaunchResult {
        val account = profile.linkedAccountId.ifBlank { null }
        val config = launchConfigForProfile(profile.id)
        return launch(LaunchContext(
            profile = profile,
            account = account,
            config = config,
            gameDir = gameDir,
            javaMajor = profile.javaMajor,
            ramMb = profile.ramMb,
            jvmArgs = profile.jvmArgs,
            customArgs = profile.customArgs
        ))
    }

    private fun launchConfigForProfile(profileId: String): LaunchConfig? {
        return null // Repository üzerinden çözümlenir; şablon için null dönülür.
    }

    private fun buildJavaCommand(ctx: LaunchContext): Array<String> {
        val jvmArgs = mutableListOf<String>()
        jvmArgs.add("-Xmx${ctx.ramMb}M")
        jvmArgs.add("-Xms${ctx.ramMb / 2}M")
        if (ctx.jvmArgs.isNotBlank()) jvmArgs.add(ctx.jvmArgs)
        jvmArgs.add("-Djava.library.path=${ctx.gameDir}/natives")
        jvmArgs.add("-Dminecraft.launcher.offline=true")
        jvmArgs.add("-Dminecraft.launcher.username=${ctx.account?.username ?: "Oyuncu"}")
        jvmArgs.add("-Dminecraft.launcher.uuid=${ctx.account?.uuid ?: UUID.randomUUID().toString()}")
        jvmArgs.add("-Dminecraft.launcher.token=${ctx.account?.offlineToken ?: ""}")
        jvmArgs.add("-cp")
        jvmArgs.add("${ctx.gameDir}/bin/*")
        jvmArgs.add("net.minecraft.client.main.Main")
        jvmArgs.add("--username")
        jvmArgs.add(ctx.account?.username ?: "Oyuncu")
        jvmArgs.add("--version")
        jvmArgs.add(ctx.profile.mcVersion)
        jvmArgs.add("--gameDir")
        jvmArgs.add(ctx.gameDir.absolutePath)
        jvmArgs.add("--assetsDir")
        jvmArgs.add("${ctx.gameDir}/assets")
        jvmArgs.add("--assetIndex")
        jvmArgs.add(ctx.profile.mcVersion)
        jvmArgs.add("--uuid")
        jvmArgs.add(ctx.account?.uuid ?: UUID.randomUUID().toString())
        jvmArgs.add("--accessToken")
        jvmArgs.add(ctx.account?.offlineToken ?: "")
        jvmArgs.add("--userType")
        jvmArgs.add("legacy")
        if (ctx.customArgs.isNotBlank()) jvmArgs.add(ctx.customArgs)
        return jvmArgs.toTypedArray()
    }

    fun validateLaunch(profile: Profile, gameDir: File): List<String> {
        val errors = mutableListOf<String>()
        if (profile.mcVersion.isBlank()) errors += "Minecraft sürümü belirtilmemiş."
        if (profile.javaMajor < 8) errors += "Java sürümü çok düşük (en az 8)."
        if (!gameDir.exists()) errors += "Oyun dizini bulunamadı: ${gameDir.absolutePath}"
        if (profile.ramMb < 1024) errors += "RAM çok az (en az 1024 MB önerilir)."
        return errors
    }
}
