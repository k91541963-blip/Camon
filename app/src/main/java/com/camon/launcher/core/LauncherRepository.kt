package com.camon.launcher.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class LauncherSettings(
    val darkTheme: Boolean = true,
    val animations: Boolean = true,
    val performanceOverlay: Boolean = true,
    val lightModeLauncher: Boolean = true,
    val autoBackup: Boolean = true,
    val backupIntervalHours: Int = 24,
    val downloadConcurrency: Int = 2,
    val wizardCompleted: Boolean = false
)

class LauncherRepository(context: Context) {

    private val prefs = context.getSharedPreferences("camon_store", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _mods = MutableStateFlow<List<ModItem>>(emptyList())
    val mods: StateFlow<List<ModItem>> = _mods.asStateFlow()

    private val _shaders = MutableStateFlow<List<ShaderPack>>(emptyList())
    val shaders: StateFlow<List<ShaderPack>> = _shaders.asStateFlow()

    private val _resourcePacks = MutableStateFlow<List<ResourcePack>>(emptyList())
    val resourcePacks: StateFlow<List<ResourcePack>> = _resourcePacks.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _history = MutableStateFlow<List<SessionRecord>>(emptyList())
    val history: StateFlow<List<SessionRecord>> = _history.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupRecord>>(emptyList())
    val backups: StateFlow<List<BackupRecord>> = _backups.asStateFlow()

    private val _touchControls = MutableStateFlow<List<TouchControl>>(emptyList())
    val touchControls: StateFlow<List<TouchControl>> = _touchControls.asStateFlow()

    private val _gamepads = MutableStateFlow<List<GamepadProfile>>(emptyList())
    val gamepads: StateFlow<List<GamepadProfile>> = _gamepads.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloads: StateFlow<List<DownloadTask>> = _downloads.asStateFlow()

    private val _crashReports = MutableStateFlow<List<CrashReport>>(emptyList())
    val crashReports: StateFlow<List<CrashReport>> = _crashReports.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    init {
        load()
    }

    // ---- Profiles ----
    fun addProfile(p: Profile) {
        _profiles.value = _profiles.value + p
        persist()
    }

    fun updateProfile(p: Profile) {
        _profiles.value = _profiles.value.map { if (it.id == p.id) p else it }
        persist()
    }

    fun deleteProfile(id: String) {
        _profiles.value = _profiles.value.filterNot { it.id == id }
        persist()
    }

    fun recordSession(profile: Profile, durationMs: Long, avgFps: Int, minFps: Int, maxFps: Int, crashed: Boolean) {
        val updated = profile.copy(
            playTimeMs = profile.playTimeMs + durationMs,
            lastPlayed = System.currentTimeMillis()
        )
        updateProfile(updated)
        _history.value = (listOf(
            SessionRecord(
                profileId = profile.id,
                profileName = profile.name,
                start = System.currentTimeMillis() - durationMs,
                durationMs = durationMs,
                avgFps = avgFps,
                minFps = minFps,
                maxFps = maxFps,
                crashed = crashed
            )
        ) + _history.value).take(100)
        persist()
    }

    // ---- Mods ----
    fun toggleMod(id: String) {
        _mods.value = _mods.value.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
        persist()
    }

    fun addMods(items: List<ModItem>) {
        _mods.value = _mods.value + items
        persist()
    }

    fun removeMod(id: String) {
        _mods.value = _mods.value.filterNot { it.id == id }
        persist()
    }

    // ---- Shaders / resource packs ----
    fun addShader(s: ShaderPack) {
        _shaders.value = _shaders.value + s
        persist()
    }

    fun removeShader(id: String) {
        _shaders.value = _shaders.value.filterNot { it.id == id }
        persist()
    }

    fun addResourcePack(r: ResourcePack) {
        _resourcePacks.value = _resourcePacks.value + r
        persist()
    }

    fun removeResourcePack(id: String) {
        _resourcePacks.value = _resourcePacks.value.filterNot { it.id == id }
        persist()
    }

    // ---- Accounts ----
    fun addAccount(a: Account) {
        val normalized = _accounts.value.map { it.copy(active = false) } + a.copy(active = true)
        _accounts.value = normalized
        persist()
    }

    fun setActiveAccount(id: String) {
        _accounts.value = _accounts.value.map { it.copy(active = it.id == id) }
        persist()
    }

    fun removeAccount(id: String) {
        _accounts.value = _accounts.value.filterNot { it.id == id }
        persist()
    }

    // ---- Backups ----
    fun addBackup(b: BackupRecord) {
        _backups.value = listOf(b) + _backups.value
        persist()
    }

    fun removeBackup(id: String) {
        _backups.value = _backups.value.filterNot { it.id == id }
        persist()
    }

    // ---- Controls ----
    fun updateTouchControl(control: TouchControl) {
        _touchControls.value = _touchControls.value.map { if (it.id == control.id) control else it }
        persist()
    }

    fun addGamepad(g: GamepadProfile) {
        _gamepads.value = _gamepads.value + g
        persist()
    }

    fun removeGamepad(id: String) {
        _gamepads.value = _gamepads.value.filterNot { it.id == id }
        persist()
    }

    // ---- Downloads ----
    fun upsertDownload(t: DownloadTask) {
        val exists = _downloads.value.any { it.id == t.id }
        _downloads.value = if (exists) {
            _downloads.value.map { if (it.id == t.id) t else it }
        } else {
            _downloads.value + t
        }
        persist()
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filterNot { it.id == id }
        persist()
    }

    // ---- Crash reports ----
    fun addCrashReport(report: CrashReport) {
        _crashReports.value = listOf(report) + _crashReports.value
        persist()
    }

    fun removeCrashReport(id: String) {
        _crashReports.value = _crashReports.value.filterNot { it.id == id }
        persist()
    }

    // ---- Settings ----
    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        _settings.value = transform(_settings.value)
        persist()
    }

    // ---- Persistence ----
    private fun persist() {
        val root = JSONObject()
        root.put("profiles", JSONArray().also { arr -> _profiles.value.forEach { arr.put(profileToJson(it)) } })
        root.put("mods", JSONArray().also { arr -> _mods.value.forEach { arr.put(modToJson(it)) } })
        root.put("shaders", JSONArray().also { arr -> _shaders.value.forEach { arr.put(shaderToJson(it)) } })
        root.put("packs", JSONArray().also { arr -> _resourcePacks.value.forEach { arr.put(packToJson(it)) } })
        root.put("accounts", JSONArray().also { arr -> _accounts.value.forEach { arr.put(accountToJson(it)) } })
        root.put("history", JSONArray().also { arr -> _history.value.forEach { arr.put(sessionToJson(it)) } })
        root.put("backups", JSONArray().also { arr -> _backups.value.forEach { arr.put(backupToJson(it)) } })
        root.put("touch", JSONArray().also { arr -> _touchControls.value.forEach { arr.put(touchToJson(it)) } })
        root.put("gamepads", JSONArray().also { arr -> _gamepads.value.forEach { arr.put(gamepadToJson(it)) } })
        root.put("downloads", JSONArray().also { arr -> _downloads.value.forEach { arr.put(downloadToJson(it)) } })
        root.put("crashes", JSONArray().also { arr -> _crashReports.value.forEach { arr.put(crashToJson(it)) } })
        root.put("settings", settingsToJson(_settings.value))
        prefs.edit().putString("state", root.toString()).apply()
    }

    private fun load() {
        seedDefaultsIfNeeded()
        val raw = prefs.getString("state", null) ?: return
        try {
            val root = JSONObject(raw)
            _profiles.value = parseArray(root, "profiles") { profileFromJson(it) }
            _mods.value = parseArray(root, "mods") { modFromJson(it) }
            _shaders.value = parseArray(root, "shaders") { shaderFromJson(it) }
            _resourcePacks.value = parseArray(root, "packs") { packFromJson(it) }
            _accounts.value = parseArray(root, "accounts") { accountFromJson(it) }
            _history.value = parseArray(root, "history") { sessionFromJson(it) }
            _backups.value = parseArray(root, "backups") { backupFromJson(it) }
            _touchControls.value = parseArray(root, "touch") { touchFromJson(it) }
            _gamepads.value = parseArray(root, "gamepads") { gamepadFromJson(it) }
            _downloads.value = parseArray(root, "downloads") { downloadFromJson(it) }
            _crashReports.value = parseArray(root, "crashes") { crashFromJson(it) }
            root.optJSONObject("settings")?.let { _settings.value = settingsFromJson(it) }
        } catch (_: Exception) {
        }
    }

    private fun <T> parseArray(root: JSONObject, key: String, mapper: (JSONObject) -> T): List<T> {
        val arr = root.optJSONArray(key) ?: return emptyList()
        val result = mutableListOf<T>()
        for (i in 0 until arr.length()) result += mapper(arr.getJSONObject(i))
        return result
    }

    private fun seedDefaultsIfNeeded() {
        if (!prefs.getBoolean("seeded", false)) {
            _profiles.value = listOf(
                Profile(name = "Vanilla 1.20.1", mcVersion = "1.20.1", loader = ModLoader.VANILLA, javaMajor = 17),
                Profile(name = "Fabric 1.20.1", mcVersion = "1.20.1", loader = ModLoader.FABRIC, loaderVersion = "0.15.11", javaMajor = 17),
                Profile(name = "Forge 1.16.5", mcVersion = "1.16.5", loader = ModLoader.FORGE, loaderVersion = "36.2.39", javaMajor = 8)
            )
            _mods.value = listOf(
                ModItem(name = "Sodium", fileName = "sodium-fabric-0.5.8.jar", version = "0.5.8", loader = ModLoader.FABRIC, mcVersions = listOf("1.20.1"), dependencies = listOf("Fabric API")),
                ModItem(name = "Fabric API", fileName = "fabric-api-0.92.2.jar", version = "0.92.2", loader = ModLoader.FABRIC, mcVersions = listOf("1.20.1")),
                ModItem(name = "Iris Shaders", fileName = "iris-1.7.0.jar", version = "1.7.0", loader = ModLoader.FABRIC, mcVersions = listOf("1.20.1"), dependencies = listOf("Sodium"))
            )
            _shaders.value = listOf(
                ShaderPack(name = "Complementary Reimagined", version = "r5.1", requiresFeatures = listOf("Compute Shaders", "Colored Lights")),
                ShaderPack(name = "BSL Shaders", version = "8.2.04", requiresFeatures = listOf("Shadow Mapping"), compatibleWithMali = false)
            )
            _resourcePacks.value = listOf(
                ResourcePack(name = "Faithful 32x", resolution = "32x", sizeMb = 48),
                ResourcePack(name = "Stay True", resolution = "16x", sizeMb = 12)
            )
            _accounts.value = listOf(Account(type = AccountType.OFFLINE, username = "Oyuncu", active = true))
            _touchControls.value = listOf(
                TouchControl("forward", "İleri", 0.20f, 0.62f),
                TouchControl("left", "Sol", 0.10f, 0.74f),
                TouchControl("right", "Sağ", 0.30f, 0.74f),
                TouchControl("back", "Geri", 0.20f, 0.86f),
                TouchControl("jump", "Zıpla", 0.82f, 0.82f),
                TouchControl("sneak", "Eğil", 0.70f, 0.88f),
                TouchControl("inventory", "Envanter", 0.90f, 0.28f),
                TouchControl("attack", "Vur", 0.88f, 0.55f),
                TouchControl("use", "Kullan", 0.74f, 0.62f)
            )
            _gamepads.value = listOf(
                GamepadProfile(
                    name = "Varsayılan kol",
                    deviceName = "Bluetooth Gamepad",
                    bindings = mapOf("A" to "Zıpla", "B" to "Eğil", "X" to "Envanter", "Y" to "Kullan")
                )
            )
            _backups.value = listOf(
                BackupRecord(worldName = "Hayatta kalma", timestamp = System.currentTimeMillis() - 3_600_000L, sizeMb = 42, automatic = true)
            )
            _downloads.value = listOf(
                DownloadTask(name = "fabric-loader-0.15.11.jar", url = "https://example/fabric.jar", totalBytes = 12_000_000, downloadedBytes = 12_000_000, status = TaskStatus.DONE),
                DownloadTask(name = "sodium-0.5.8.jar", url = "https://example/sodium.jar", totalBytes = 1_800_000, downloadedBytes = 900_000, status = TaskStatus.PAUSED, attempts = 1)
            )
            _history.value = listOf(
                SessionRecord(profileId = _profiles.value[1].id, profileName = "Fabric 1.20.1", start = System.currentTimeMillis() - 7_200_000L, durationMs = 5_400_000L, avgFps = 58, minFps = 34, maxFps = 72, crashed = false),
                SessionRecord(profileId = _profiles.value[2].id, profileName = "Forge 1.16.5", start = System.currentTimeMillis() - 86_400_000L, durationMs = 1_200_000L, avgFps = 41, minFps = 12, maxFps = 60, crashed = true)
            )
            prefs.edit().putBoolean("seeded", true).apply()
            persist()
        }
    }

    // ---- JSON mappers ----
    private fun profileToJson(p: Profile) = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("mcVersion", p.mcVersion)
        put("loader", p.loader.name); put("loaderVersion", p.loaderVersion)
        put("ramMb", p.ramMb); put("javaMajor", p.javaMajor); put("renderer", p.renderer.name)
        put("performance", p.performance.name); put("jvmArgs", p.jvmArgs); put("controlLayout", p.controlLayout)
        put("createdAt", p.createdAt); put("playTimeMs", p.playTimeMs); put("lastPlayed", p.lastPlayed)
    }

    private fun profileFromJson(o: JSONObject) = Profile(
        id = o.optString("id"), name = o.optString("name"), mcVersion = o.optString("mcVersion"),
        loader = runCatching { ModLoader.valueOf(o.optString("loader")) }.getOrDefault(ModLoader.VANILLA),
        loaderVersion = o.optString("loaderVersion"), ramMb = o.optInt("ramMb", 2048),
        javaMajor = o.optInt("javaMajor", 17),
        renderer = runCatching { RendererType.valueOf(o.optString("renderer")) }.getOrDefault(RendererType.OPENGL),
        performance = runCatching { PerformanceProfile.valueOf(o.optString("performance")) }.getOrDefault(PerformanceProfile.BALANCED),
        jvmArgs = o.optString("jvmArgs"), controlLayout = o.optString("controlLayout", "varsayılan"),
        createdAt = o.optLong("createdAt"), playTimeMs = o.optLong("playTimeMs"), lastPlayed = o.optLong("lastPlayed")
    )

    private fun modToJson(m: ModItem) = JSONObject().apply {
        put("id", m.id); put("name", m.name); put("fileName", m.fileName); put("version", m.version)
        put("loader", m.loader.name); put("mcVersions", JSONArray(m.mcVersions)); put("enabled", m.enabled)
        put("dependencies", JSONArray(m.dependencies)); put("side", m.side)
    }

    private fun modFromJson(o: JSONObject) = ModItem(
        id = o.optString("id"), name = o.optString("name"), fileName = o.optString("fileName"),
        version = o.optString("version"),
        loader = runCatching { ModLoader.valueOf(o.optString("loader")) }.getOrDefault(ModLoader.VANILLA),
        mcVersions = o.optJSONArray("mcVersions")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        enabled = o.optBoolean("enabled", true),
        dependencies = o.optJSONArray("dependencies")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        side = o.optString("side", "both")
    )

    private fun shaderToJson(s: ShaderPack) = JSONObject().apply {
        put("id", s.id); put("name", s.name); put("version", s.version)
        put("features", JSONArray(s.requiresFeatures)); put("mali", s.compatibleWithMali)
    }

    private fun shaderFromJson(o: JSONObject) = ShaderPack(
        id = o.optString("id"), name = o.optString("name"), version = o.optString("version"),
        requiresFeatures = o.optJSONArray("features")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        compatibleWithMali = o.optBoolean("mali", true)
    )

    private fun packToJson(r: ResourcePack) = JSONObject().apply {
        put("id", r.id); put("name", r.name); put("resolution", r.resolution); put("sizeMb", r.sizeMb)
    }

    private fun packFromJson(o: JSONObject) = ResourcePack(
        id = o.optString("id"), name = o.optString("name"),
        resolution = o.optString("resolution"), sizeMb = o.optInt("sizeMb")
    )

    private fun accountToJson(a: Account) = JSONObject().apply {
        put("id", a.id); put("type", a.type.name); put("username", a.username)
        put("uuid", a.uuid); put("active", a.active)
    }

    private fun accountFromJson(o: JSONObject) = Account(
        id = o.optString("id"),
        type = runCatching { AccountType.valueOf(o.optString("type")) }.getOrDefault(AccountType.OFFLINE),
        username = o.optString("username"), uuid = o.optString("uuid"), active = o.optBoolean("active")
    )

    private fun sessionToJson(s: SessionRecord) = JSONObject().apply {
        put("id", s.id); put("profileId", s.profileId); put("profileName", s.profileName)
        put("start", s.start); put("durationMs", s.durationMs); put("avgFps", s.avgFps)
        put("minFps", s.minFps); put("maxFps", s.maxFps); put("crashed", s.crashed)
    }

    private fun sessionFromJson(o: JSONObject) = SessionRecord(
        id = o.optString("id"), profileId = o.optString("profileId"), profileName = o.optString("profileName"),
        start = o.optLong("start"), durationMs = o.optLong("durationMs"), avgFps = o.optInt("avgFps"),
        minFps = o.optInt("minFps"), maxFps = o.optInt("maxFps"), crashed = o.optBoolean("crashed")
    )

    private fun backupToJson(b: BackupRecord) = JSONObject().apply {
        put("id", b.id); put("worldName", b.worldName); put("timestamp", b.timestamp)
        put("sizeMb", b.sizeMb); put("automatic", b.automatic)
    }

    private fun backupFromJson(o: JSONObject) = BackupRecord(
        id = o.optString("id"), worldName = o.optString("worldName"), timestamp = o.optLong("timestamp"),
        sizeMb = o.optInt("sizeMb"), automatic = o.optBoolean("automatic")
    )

    private fun touchToJson(t: TouchControl) = JSONObject().apply {
        put("id", t.id); put("label", t.label); put("x", t.x.toDouble()); put("y", t.y.toDouble())
        put("size", t.size.toDouble()); put("alpha", t.alpha.toDouble()); put("visible", t.visible)
    }

    private fun touchFromJson(o: JSONObject) = TouchControl(
        id = o.optString("id"), label = o.optString("label"),
        x = o.optDouble("x", 0.5).toFloat(), y = o.optDouble("y", 0.5).toFloat(),
        size = o.optDouble("size", 1.0).toFloat(), alpha = o.optDouble("alpha", 1.0).toFloat(),
        visible = o.optBoolean("visible", true)
    )

    private fun gamepadToJson(g: GamepadProfile) = JSONObject().apply {
        put("id", g.id); put("name", g.name); put("deviceName", g.deviceName)
        val b = JSONObject(); g.bindings.forEach { (k, v) -> b.put(k, v) }; put("bindings", b)
    }

    private fun gamepadFromJson(o: JSONObject) = GamepadProfile(
        id = o.optString("id"), name = o.optString("name"), deviceName = o.optString("deviceName"),
        bindings = o.optJSONObject("bindings")?.let { b -> b.keys().asSequence().associateWith { b.optString(it) } } ?: emptyMap()
    )

    private fun downloadToJson(t: DownloadTask) = JSONObject().apply {
        put("id", t.id); put("name", t.name); put("url", t.url); put("totalBytes", t.totalBytes)
        put("downloadedBytes", t.downloadedBytes); put("sha1", t.sha1); put("status", t.status.name)
        put("attempts", t.attempts)
    }

    private fun downloadFromJson(o: JSONObject) = DownloadTask(
        id = o.optString("id"), name = o.optString("name"), url = o.optString("url"),
        totalBytes = o.optLong("totalBytes"), downloadedBytes = o.optLong("downloadedBytes"),
        sha1 = o.optString("sha1"),
        status = runCatching { TaskStatus.valueOf(o.optString("status")) }.getOrDefault(TaskStatus.PENDING),
        attempts = o.optInt("attempts")
    )

    private fun crashToJson(c: CrashReport) = JSONObject().apply {
        put("id", c.id); put("profileId", c.profileId); put("profileName", c.profileName)
        put("timestamp", c.timestamp); put("title", c.title); put("summary", c.summary)
        put("causes", JSONArray(c.causes)); put("logExcerpt", c.logExcerpt)
    }

    private fun crashFromJson(o: JSONObject) = CrashReport(
        id = o.optString("id"), profileId = o.optString("profileId"), profileName = o.optString("profileName"),
        timestamp = o.optLong("timestamp"), title = o.optString("title"), summary = o.optString("summary"),
        causes = o.optJSONArray("causes")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        logExcerpt = o.optString("logExcerpt")
    )

    private fun settingsToJson(s: LauncherSettings) = JSONObject().apply {
        put("darkTheme", s.darkTheme); put("animations", s.animations)
        put("performanceOverlay", s.performanceOverlay); put("lightModeLauncher", s.lightModeLauncher)
        put("autoBackup", s.autoBackup); put("backupIntervalHours", s.backupIntervalHours)
        put("downloadConcurrency", s.downloadConcurrency); put("wizardCompleted", s.wizardCompleted)
    }

    private fun settingsFromJson(o: JSONObject) = LauncherSettings(
        darkTheme = o.optBoolean("darkTheme", true), animations = o.optBoolean("animations", true),
        performanceOverlay = o.optBoolean("performanceOverlay", true), lightModeLauncher = o.optBoolean("lightModeLauncher", true),
        autoBackup = o.optBoolean("autoBackup", true), backupIntervalHours = o.optInt("backupIntervalHours", 24),
        downloadConcurrency = o.optInt("downloadConcurrency", 2), wizardCompleted = o.optBoolean("wizardCompleted", false)
    )
}
