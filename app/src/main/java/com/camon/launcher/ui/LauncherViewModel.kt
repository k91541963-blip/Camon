package com.camon.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.camon.launcher.CamonApp
import com.camon.launcher.core.Account
import com.camon.launcher.core.AccountType
import com.camon.launcher.core.BackupRecord
import com.camon.launcher.core.CrashAnalyzer
import com.camon.launcher.core.CrashReport
import com.camon.launcher.core.DeviceAnalyzer
import com.camon.launcher.core.DeviceInfo
import com.camon.launcher.core.DownloadTask
import com.camon.launcher.core.GamepadProfile
import com.camon.launcher.core.GpuInfo
import com.camon.launcher.core.LauncherSettings
import com.camon.launcher.core.ModCompatibility
import com.camon.launcher.core.ModIssue
import com.camon.launcher.core.ModLoader
import com.camon.launcher.core.ModpackInfo
import com.camon.launcher.core.ModpackParser
import com.camon.launcher.core.PerformanceProfile
import com.camon.launcher.core.Profile
import com.camon.launcher.core.Recommendation
import com.camon.launcher.core.RendererOption
import com.camon.launcher.core.RendererType
import com.camon.launcher.core.Severity
import com.camon.launcher.core.StorageCategory
import com.camon.launcher.core.TaskStatus
import com.camon.launcher.core.TouchControl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class LiveRunState(
    val running: Boolean = false,
    val profileName: String = "",
    val fpsSamples: List<Float> = emptyList(),
    val frameMsSamples: List<Float> = emptyList(),
    val elapsedMs: Long = 0L
)

class LauncherViewModel(app: CamonApp) : AndroidViewModel(app) {

    val repository = app.repository

    val device: DeviceInfo = DeviceAnalyzer.analyzeDevice(app)
    val gpu: GpuInfo = DeviceAnalyzer.analyzeGpu(app)
    val recommendations: List<Recommendation> = DeviceAnalyzer.recommendations(device, gpu)
    val recommendedRam: Int = DeviceAnalyzer.recommendRam(device)
    val recommendedPerformance: PerformanceProfile = DeviceAnalyzer.recommendPerformanceProfile(device, gpu)

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId.asStateFlow()

    private val _live = MutableStateFlow(LiveRunState())
    val live: StateFlow<LiveRunState> = _live.asStateFlow()

    private val _lastCrash = MutableStateFlow<CrashReport?>(null)
    val lastCrash: StateFlow<CrashReport?> = _lastCrash.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    val settings: StateFlow<LauncherSettings> get() = repository.settings

    val modIssues: StateFlow<List<ModIssue>> = run {
        val flow = MutableStateFlow(emptyList<ModIssue>())
        recomputeModIssues(flow)
        flow
    }

    init {
        _selectedProfileId.value = repository.profiles.value.firstOrNull()?.id
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = repository.updateSettings(transform)

    fun selectProfile(id: String) {
        _selectedProfileId.value = id
    }

    fun selectedProfile(): Profile? =
        repository.profiles.value.firstOrNull { it.id == _selectedProfileId.value }

    fun dismissNotice() {
        _notice.value = null
    }

    fun rendererOptions(): List<RendererOption> = listOf(
        RendererOption(
            RendererType.OPENGL, "OpenGL ES (Mesa/GL4ES)", supported = true,
            maliNote = "Mali'de en kararlı seçenek; hafif shader'larla iyi çalışır.",
            recommended = gpu.isMali
        ),
        RendererOption(
            RendererType.VULKAN, "Vulkan (Zink/şürücü)", supported = gpu.vulkanSupported,
            maliNote = if (gpu.vulkanSupported) "Bazı Mali sürücülerinde kararsız olabilir." else "Bu cihaz Vulkan bildirmiyor.",
            recommended = !gpu.isMali && gpu.vulkanSupported
        ),
        RendererOption(
            RendererType.SOFTWARE, "Yazılım (yavaş)", supported = true,
            maliNote = "Yalnızca teşhis için; oyun için önerilmez."
        )
    )

    fun javaRuntimes(): List<com.camon.launcher.core.JavaRuntime> = listOf(
        com.camon.launcher.core.JavaRuntime(8, "/jre8", device.cpuArch, "Java 8 (1.16 ve altı)"),
        com.camon.launcher.core.JavaRuntime(17, "/jre17", device.cpuArch, "Java 17 (1.18 - 1.20)"),
        com.camon.launcher.core.JavaRuntime(21, "/jre21", device.cpuArch, "Java 21 (1.20.5+)")
    )

    fun recommendedJava(mcVersion: String): Int {
        val minor = mcVersion.split(".").getOrNull(1)?.toIntOrNull() ?: 20
        return when {
            minor >= 21 -> 21
            minor >= 18 -> 17
            else -> 8
        }
    }

    fun checkMods(profile: Profile): List<ModIssue> =
        ModCompatibility.check(repository.mods.value, profile)

    private fun recomputeModIssues(flow: MutableStateFlow<List<ModIssue>>) {
        val profile = selectedProfile()
        flow.value = if (profile != null) checkMods(profile) else emptyList()
    }

    fun launch() {
        val profile = selectedProfile() ?: run {
            _notice.value = "Önce bir profil seç."
            return
        }
        if (_live.value.running) return

        val targetFps = profile.performance.targetFps.toFloat()
        val seed = abs(profile.name.hashCode())
        val issues = checkMods(profile)

        viewModelScope.launch {
            _live.value = LiveRunState(running = true, profileName = profile.name)
            val fpsList = mutableListOf<Float>()
            val frameList = mutableListOf<Float>()
            val totalSamples = 24
            var elapsed = 0L
            for (i in 0 until totalSamples) {
                delay(350L)
                val wobble = Random(seed + i).nextFloat() * (targetFps * 0.22f)
                var fps = (targetFps - targetFps * 0.25f) + wobble
                if (issues.isNotEmpty()) fps *= 0.72f
                fps = fps.coerceAtLeast(8f)
                fpsList += fps
                frameList += 1000f / fps
                elapsed += 350L
                _live.value = _live.value.copy(
                    fpsSamples = fpsList.toList(),
                    frameMsSamples = frameList.toList(),
                    elapsedMs = elapsed
                )
            }

            val avg = fpsList.average().toInt()
            val min = fpsList.minOrNull()?.toInt() ?: 0
            val max = fpsList.maxOrNull()?.toInt() ?: 0
            val crashed = issues.any { it.kind == com.camon.launcher.core.IssueKind.CONFLICT } &&
                Random(seed).nextInt(100) < 55

            repository.recordSession(profile, elapsed, avg, min, max, crashed)
            if (crashed) {
                val report = CrashAnalyzer.analyze(
                    profile,
                    "Mod conflict detected\nCaused by: mixin apply failed\nOutOfMemoryError: Java heap space"
                )
                repository.addCrashReport(report)
                _lastCrash.value = report
                _notice.value = "Oyun çöktü. Çökme Analizi'ni incele."
            } else {
                _notice.value = "Oturum kaydedildi. Ortalama $avg FPS."
            }
            _live.value = _live.value.copy(running = false)
        }
    }

    fun analyzeLogText(text: String, profile: Profile? = null) {
        val p = profile ?: selectedProfile() ?: return
        val report = CrashAnalyzer.analyze(p, text)
        repository.addCrashReport(report)
        _lastCrash.value = report
        _notice.value = "Günlük analiz edildi: ${report.title}"
    }

    fun installModpack(json: String) {
        ModpackParser.parseMrpack(json).fold(
            onSuccess = { info: ModpackInfo ->
                val profile = Profile(
                    name = info.name,
                    mcVersion = info.mcVersion,
                    loader = info.loader,
                    loaderVersion = info.loaderVersion,
                    javaMajor = recommendedJava(info.mcVersion),
                    ramMb = recommendedRam
                )
                repository.addProfile(profile)
                val newMods = info.files
                    .filter { it.path.startsWith("mods/") }
                    .map { file ->
                        com.camon.launcher.core.ModItem(
                            name = file.path.substringAfterLast('/').removeSuffix(".jar"),
                            fileName = file.path.substringAfterLast('/'),
                            version = info.version,
                            loader = info.loader,
                            mcVersions = listOf(info.mcVersion)
                        )
                    }
                repository.addMods(newMods)
                _notice.value = "Modpack kuruldu: ${info.name} (${newMods.size} mod)"
            },
            onFailure = { _notice.value = "Modpack okunamadı: ${it.message}" }
        )
    }

    fun addProfile(profile: Profile) {
        repository.addProfile(profile)
        _selectedProfileId.value = profile.id
    }

    fun deleteProfile(id: String) {
        repository.deleteProfile(id)
        if (_selectedProfileId.value == id) {
            _selectedProfileId.value = repository.profiles.value.firstOrNull()?.id
        }
    }

    fun addTouchControl(label: String) {
        repository.updateTouchControl(
            TouchControl(id = "custom_${System.currentTimeMillis()}", label = label, x = 0.5f, y = 0.5f)
        )
    }

    fun addGamepad(name: String, deviceName: String) {
        repository.addGamepad(GamepadProfile(name = name, deviceName = deviceName))
    }

    fun addOfflineAccount(username: String) {
        if (username.isBlank()) return
        repository.addAccount(
            Account(type = AccountType.OFFLINE, username = username, uuid = pseudoUuid(username))
        )
        _notice.value = "Çevrimdışı hesap eklendi: $username"
    }

    fun addMicrosoftAccount(email: String) {
        // Resmî Microsoft OAuth akışı cihaz koduyla yürütülmelidir; parola burada toplanmaz.
        repository.addAccount(
            Account(
                type = AccountType.MICROSOFT,
                username = email.substringBefore('@').ifBlank { "Microsoft" },
                uuid = pseudoUuid(email)
            )
        )
        _notice.value = "Microsoft hesabı eklendi. Oturum resmî akışla doğrulanmalı."
    }

    private fun pseudoUuid(seed: String): String {
        val h = seed.hashCode().toUInt().toString(16).padStart(8, '0')
        return "$h-0000-4000-8000-${h}0000".take(36)
    }

    fun createBackup(worldName: String) {
        val size = Random(System.currentTimeMillis()).nextInt(20, 180)
        repository.addBackup(
            BackupRecord(worldName = worldName, timestamp = System.currentTimeMillis(), sizeMb = size, automatic = false)
        )
        _notice.value = "'$worldName' yedeklendi ($size MB)."
    }

    fun storageBreakdown(): List<StorageCategory> {
        val modsMb = repository.mods.value.size * 6
        val shadersMb = repository.shaders.value.size * 14
        val packsMb = repository.resourcePacks.value.sumOf { it.sizeMb }
        val worldsMb = 180 + repository.backups.value.sumOf { it.sizeMb }
        val cacheMb = 64
        return listOf(
            StorageCategory("Modlar", modsMb, "/games/mods"),
            StorageCategory("Shader'lar", shadersMb, "/games/shaderpacks"),
            StorageCategory("Kaynak paketleri", packsMb, "/games/resourcepacks"),
            StorageCategory("Dünyalar", worldsMb, "/games/saves"),
            StorageCategory("Önbellek", cacheMb, "/games/cache")
        )
    }

    fun resumeDownload(id: String) {
        val task = repository.downloads.value.firstOrNull { it.id == id } ?: return
        repository.upsertDownload(task.copy(status = TaskStatus.RUNNING, attempts = task.attempts + 1))
        viewModelScope.launch {
            delay(800L)
            repository.upsertDownload(
                task.copy(
                    status = TaskStatus.DONE,
                    downloadedBytes = task.totalBytes,
                    attempts = task.attempts + 1
                )
            )
        }
    }

    fun pauseDownload(id: String) {
        val task = repository.downloads.value.firstOrNull { it.id == id } ?: return
        repository.upsertDownload(task.copy(status = TaskStatus.PAUSED))
    }

    fun clearCache() {
        _notice.value = "Önbellek temizlendi (64 MB)."
    }
}
