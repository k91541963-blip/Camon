package com.camon.launcher.core

import java.util.UUID

enum class ModLoader(val display: String) {
    VANILLA("Vanilla"),
    FABRIC("Fabric"),
    FORGE("Forge"),
    NEOFORGE("NeoForge"),
    QUILT("Quilt")
}

enum class RendererType(val display: String) {
    OPENGL("OpenGL ES"),
    VULKAN("Vulkan"),
    SOFTWARE("Yazılım (yavaş)")
}

enum class AccountType(val display: String) {
    MICROSOFT("Microsoft"),
    OFFLINE("Çevrimdışı")
}

enum class PerformanceProfile(val display: String, val targetFps: Int, val renderDistance: Int) {
    POWER_SAVE("Güç tasarrufu", 30, 6),
    BALANCED("Dengeli", 60, 10),
    PERFORMANCE("Performans", 90, 8),
    QUALITY("Görsel kalite", 60, 16)
}

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mcVersion: String,
    val loader: ModLoader,
    val loaderVersion: String = "",
    val ramMb: Int = 2048,
    val javaMajor: Int = 17,
    val renderer: RendererType = RendererType.OPENGL,
    val performance: PerformanceProfile = PerformanceProfile.BALANCED,
    val jvmArgs: String = "",
    val controlLayout: String = "varsayılan",
    val createdAt: Long = System.currentTimeMillis(),
    val playTimeMs: Long = 0L,
    val lastPlayed: Long = 0L
)

data class ModItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileName: String,
    val version: String,
    val loader: ModLoader,
    val mcVersions: List<String>,
    val enabled: Boolean = true,
    val dependencies: List<String> = emptyList(),
    val side: String = "both"
)

data class ShaderPack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: String,
    val requiresFeatures: List<String> = emptyList(),
    val compatibleWithMali: Boolean = true
)

data class ResourcePack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val resolution: String,
    val sizeMb: Int
)

data class RendererOption(
    val type: RendererType,
    val name: String,
    val supported: Boolean,
    val maliNote: String,
    val recommended: Boolean = false
)

data class JavaRuntime(
    val major: Int,
    val path: String,
    val arch: String,
    val name: String
)

data class GpuInfo(
    val vendor: String,
    val model: String,
    val isMali: Boolean,
    val glesVersion: String,
    val vulkanSupported: Boolean,
    val driverVersion: String,
    val maxTextureSize: Int
)

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val cpuArch: String,
    val cores: Int,
    val cpuMaxMhz: Int,
    val totalRamMb: Int,
    val availRamMb: Int,
    val lowRamDevice: Boolean,
    val socModel: String
)

data class CrashReport(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val profileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val summary: String,
    val causes: List<String>,
    val logExcerpt: String
)

data class SessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val profileName: String,
    val start: Long,
    val durationMs: Long,
    val avgFps: Int,
    val minFps: Int,
    val maxFps: Int,
    val crashed: Boolean
)

data class BackupRecord(
    val id: String = UUID.randomUUID().toString(),
    val worldName: String,
    val timestamp: Long,
    val sizeMb: Int,
    val automatic: Boolean
)

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val type: AccountType,
    val username: String,
    val uuid: String = "",
    val active: Boolean = false
)

data class TouchControl(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val size: Float = 1f,
    val alpha: Float = 1f,
    val visible: Boolean = true
)

data class GamepadProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val deviceName: String,
    val bindings: Map<String, String> = emptyMap()
)

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val sha1: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val attempts: Int = 0
)

enum class TaskStatus(val display: String) {
    PENDING("Bekliyor"),
    RUNNING("İndiriliyor"),
    PAUSED("Duraklatıldı"),
    VERIFYING("Doğrulanıyor"),
    DONE("Tamamlandı"),
    FAILED("Başarısız")
}

data class StorageCategory(
    val name: String,
    val sizeMb: Int,
    val path: String
)

data class Recommendation(
    val title: String,
    val detail: String,
    val severity: Severity
)

enum class Severity(val display: String) {
    OK("İyi"),
    INFO("Bilgi"),
    WARN("Uyarı"),
    DANGER("Kritik")
}

data class ModIssue(
    val modName: String,
    val kind: IssueKind,
    val detail: String
)

enum class IssueKind(val display: String) {
    MISSING_DEPENDENCY("Eksik bağımlılık"),
    VERSION_MISMATCH("Sürüm uyuşmazlığı"),
    CONFLICT("Mod çakışması"),
    WRONG_LOADER("Yanlış yükleyici")
}
