package com.camon.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class Dest(val title: String, val glyph: String, val group: String) {
    HOME("Ana Sayfa", "◆", "Genel"),
    PROFILES("Profiller", "▤", "Genel"),
    PERFORMANCE("Performans", "▲", "Performans"),
    CRASH("Çökme Analizi", "✚", "Performans"),
    HISTORY("Oturum Geçmişi", "≡", "Performans"),
    MODS("Mod Yöneticisi", "❖", "İçerik"),
    MODPACKS("Modpack Kurulumu", "⬇", "İçerik"),
    SHADERS("Shader & Paketler", "✦", "İçerik"),
    BACKUPS("Dünya Yedekleri", "⛁", "İçerik"),
    STORAGE("Depolama", "▣", "İçerik"),
    DOWNLOADS("İndirmeler", "↓", "İçerik"),
    RENDERERS("Renderer Yöneticisi", "◈", "Cihaz"),
    JAVA_RAM("Java & RAM", "⚙", "Cihaz"),
    MALI("Mali GPU", "◉", "Cihaz"),
    CONTROLS("Dokunmatik Kontroller", "✜", "Cihaz"),
    GAMEPAD("Gamepad", "⊙", "Cihaz"),
    ACCOUNTS("Hesaplar", "☺", "Hesap"),
    WIZARD("Kurulum Sihirbazı", "★", "Sistem"),
    SETTINGS("Ayarlar", "⚒", "Sistem")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamonRoot(vm: LauncherViewModel) {
    var current by remember { mutableStateOf(Dest.HOME) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val notice by vm.notice.collectAsStateCompat()

    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice!!)
            vm.dismissNotice()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("C", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Camon", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Minecraft Java başlatıcı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val groups = Dest.values().groupBy { it.group }
                    for ((group, items) in groups) {
                        Text(
                            group.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
                        )
                        items.forEach { dest ->
                            val selected = dest == current
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surface,
                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        current = dest
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dest.glyph, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    dest.title,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(current.title) },
                    navigationIcon = {
                        Text(
                            "☰",
                            modifier = Modifier
                                .clickable { scope.launch { drawerState.open() } }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (current) {
                    Dest.HOME -> HomeScreen(vm) { current = it }
                    Dest.PROFILES -> ProfilesScreen(vm)
                    Dest.PERFORMANCE -> PerformanceScreen(vm)
                    Dest.CRASH -> CrashScreen(vm)
                    Dest.HISTORY -> HistoryScreen(vm)
                    Dest.MODS -> ModsScreen(vm)
                    Dest.MODPACKS -> ModpackScreen(vm)
                    Dest.SHADERS -> ShaderScreen(vm)
                    Dest.BACKUPS -> BackupScreen(vm)
                    Dest.STORAGE -> StorageScreen(vm)
                    Dest.DOWNLOADS -> DownloadsScreen(vm)
                    Dest.RENDERERS -> RendererScreen(vm)
                    Dest.JAVA_RAM -> JavaRamScreen(vm)
                    Dest.MALI -> MaliScreen(vm)
                    Dest.CONTROLS -> ControlsScreen(vm)
                    Dest.GAMEPAD -> GamepadScreen(vm)
                    Dest.ACCOUNTS -> AccountsScreen(vm)
                    Dest.WIZARD -> WizardScreen(vm)
                    Dest.SETTINGS -> SettingsScreen(vm)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
