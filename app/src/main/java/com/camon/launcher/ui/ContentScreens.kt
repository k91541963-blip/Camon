package com.camon.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camon.launcher.core.DownloadTask
import com.camon.launcher.core.IssueKind
import com.camon.launcher.core.ResourcePack
import com.camon.launcher.core.ShaderPack
import com.camon.launcher.core.TaskStatus

@Composable
fun ModsScreen(vm: LauncherViewModel) {
    val mods by vm.repository.mods.collectAsStateCompat()
    val selectedId by vm.selectedProfileId.collectAsStateCompat()
    val profiles by vm.repository.profiles.collectAsStateCompat()
    val profile = profiles.firstOrNull { it.id == selectedId }
    val issues = if (profile != null) vm.checkMods(profile) else emptyList()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Akıllı Mod Yöneticisi", "${mods.size} mod • ${mods.count { it.enabled }} etkin")
        Panel(title = "Uyumluluk denetimi") {
            if (profile == null) {
                Text("Profil seçili değil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (issues.isEmpty()) {
                Pill("Tüm modlar uyumlu", CamonColors.good)
            } else {
                Text(
                    "${issues.size} sorun bulundu",
                    color = CamonColors.warn,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                issues.forEach { issue ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Pill(issue.kind.display, issueColor(issue.kind))
                            Text(issue.modName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(issue.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        mods.forEach { mod ->
            Panel(title = mod.name + " " + mod.version) {
                Text(mod.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    mod.loader.display + " • " + mod.mcVersions.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (mod.enabled) "Etkin" else "Devre dışı", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = mod.enabled, onCheckedChange = { vm.repository.toggleMod(mod.id) })
                }
                TextButton(onClick = { vm.repository.removeMod(mod.id) }) { Text("Sil") }
            }
        }
        if (mods.isEmpty()) EmptyState("Mod kurulu değil.")
    }
}

private fun issueColor(kind: IssueKind) = when (kind) {
    IssueKind.MISSING_DEPENDENCY -> CamonColors.bad
    IssueKind.VERSION_MISMATCH -> CamonColors.warn
    IssueKind.CONFLICT -> CamonColors.warn
    IssueKind.WRONG_LOADER -> CamonColors.info
}

@Composable
fun ModpackScreen(vm: LauncherViewModel) {
    var json by remember { mutableStateOf("") }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Tek Tıkla Modpack Kurulumu", "Modrinth .mrpack içe aktar")
        Panel(title = "Kaynak") {
            Text(
                "Modrinth .mrpack dosyasının 'modrinth.index.json' içeriğini yapıştır. " +
                    "Launcher, gerekli mod dosyalarını, yükleyiciyi ve sürümü okuyup profil oluşturur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Panel(title = "mrpack JSON") {
            OutlinedTextField(
                value = json,
                onValueChange = { json = it },
                label = { Text("modrinth.index.json") },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.installModpack(json) }, enabled = json.isNotBlank()) { Text("Modpack kur") }
        }
    }
}

@Composable
fun ShaderScreen(vm: LauncherViewModel) {
    val shaders by vm.repository.shaders.collectAsStateCompat()
    val packs by vm.repository.resourcePacks.collectAsStateCompat()
    val gpu = vm.gpu

    var showShader by remember { mutableStateOf(false) }
    var showPack by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Shader & Kaynak Paketi", "Profil bazlı görsel yapılandırma")
        Button(onClick = { showShader = true }) { Text("+ Shader") }
        Spacer(Modifier.height(6.dp))
        shaders.forEach { shader ->
            Panel(title = shader.name + " " + shader.version) {
                if (shader.requiresFeatures.isNotEmpty()) {
                    Text(shader.requiresFeatures.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val ok = shader.compatibleWithMali || !gpu.isMali
                Pill(if (ok) "Mali uyumlu" else "Mali'de riskli", if (ok) CamonColors.good else CamonColors.warn)
                TextButton(onClick = { vm.repository.removeShader(shader.id) }) { Text("Sil") }
            }
        }

        SectionHeader("Kaynak paketleri")
        Button(onClick = { showPack = true }) { Text("+ Kaynak paketi") }
        Spacer(Modifier.height(6.dp))
        packs.forEach { pack ->
            Panel(title = pack.name) {
                Text("${pack.resolution} • ${pack.sizeMb} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { vm.repository.removeResourcePack(pack.id) }) { Text("Sil") }
            }
        }
    }

    if (showShader) {
        var name by remember { mutableStateOf("") }
        var version by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showShader = false },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.repository.addShader(ShaderPack(name = name, version = version.ifBlank { "1.0" }))
                    showShader = false
                }) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { showShader = false }) { Text("İptal") } },
            title = { Text("Shader ekle") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ad") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Sürüm") })
                }
            }
        )
    }
    if (showPack) {
        var name by remember { mutableStateOf("") }
        var res by remember { mutableStateOf("16x") }
        AlertDialog(
            onDismissRequest = { showPack = false },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.repository.addResourcePack(ResourcePack(name = name, resolution = res, sizeMb = 20))
                    showPack = false
                }) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { showPack = false }) { Text("İptal") } },
            title = { Text("Kaynak paketi ekle") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ad") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = res, onValueChange = { res = it }, label = { Text("Çözünürlük") })
                }
            }
        )
    }
}

@Composable
fun BackupScreen(vm: LauncherViewModel) {
    val backups by vm.repository.backups.collectAsStateCompat()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Otomatik Dünya Yedekleme", "Modpack değişiminden önce güvenlik kopyası")
        Button(onClick = { showAdd = true }) { Text("+ Yedek oluştur") }
        Spacer(Modifier.height(8.dp))
        backups.forEach { backup ->
            Panel(title = backup.worldName) {
                Text(
                    "${formatTimestamp(backup.timestamp)} • ${backup.sizeMb} MB • " + if (backup.automatic) "otomatik" else "manuel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.createBackup(backup.worldName) }) { Text("Geri yükle") }
                    TextButton(onClick = { vm.repository.removeBackup(backup.id) }) { Text("Sil") }
                }
            }
        }
        if (backups.isEmpty()) EmptyState("Yedek yok.")
    }

    if (showAdd) {
        var world by remember { mutableStateOf("Dünyam") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = { TextButton(onClick = { vm.createBackup(world); showAdd = false }) { Text("Yedekle") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } },
            title = { Text("Dünya yedekle") },
            text = { OutlinedTextField(value = world, onValueChange = { world = it }, label = { Text("Dünya adı") }) }
        )
    }
}

@Composable
fun StorageScreen(vm: LauncherViewModel) {
    val categories = vm.storageBreakdown()
    val total = categories.sumOf { it.sizeMb }.coerceAtLeast(1)
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Depolama ve Dosya Yöneticisi", "Toplam ${total} MB kullanım")
        Panel(title = "Kategori kullanımı") {
            categories.forEach { cat ->
                BarRow(cat.name, cat.sizeMb.toFloat() / total, "${cat.sizeMb} MB", CamonColors.info)
                Text(cat.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Panel(title = "Önbellek") {
            Text(
                "Önbellek dosyalarını silmek oyun ayarlarını etkilemez. " +
                    "Silmeden önce onay istenir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { confirmClear = true }) { Text("Önbelleği temizle") }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            confirmButton = { TextButton(onClick = { vm.clearCache(); confirmClear = false }) { Text("Temizle") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("İptal") } },
            title = { Text("Emin misin?") },
            text = { Text("Önbellek dosyaları silinecek. Bu işlem geri alınamaz.") }
        )
    }
}

@Composable
fun DownloadsScreen(vm: LauncherViewModel) {
    val downloads by vm.repository.downloads.collectAsStateCompat()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Kesintiye Dayanıklı İndirimler", "Kaldığı yerden devam, bütünlük doğrulama")
        downloads.forEach { task ->
            DownloadRow(task, vm)
        }
        if (downloads.isEmpty()) EmptyState("İndirme yok.")
    }
}

@Composable
private fun DownloadRow(task: DownloadTask, vm: LauncherViewModel) {
    val fraction = if (task.totalBytes > 0) task.downloadedBytes.toFloat() / task.totalBytes else 0f
    Panel(title = task.name) {
        BarRow(
            task.status.display,
            fraction,
            "${task.downloadedBytes / 1_000_000} / ${task.totalBytes / 1_000_000} MB",
            if (task.status == TaskStatus.DONE) CamonColors.good else CamonColors.info
        )
        if (task.attempts > 0) {
            Text("Deneme: ${task.attempts}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (task.status) {
                TaskStatus.PAUSED, TaskStatus.FAILED, TaskStatus.PENDING ->
                    TextButton(onClick = { vm.resumeDownload(task.id) }) { Text("Devam et") }
                TaskStatus.RUNNING ->
                    TextButton(onClick = { vm.pauseDownload(task.id) }) { Text("Duraklat") }
                else -> {}
            }
            TextButton(onClick = { vm.repository.removeDownload(task.id) }) { Text("Kaldır") }
        }
    }
}

fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val min = diff / 60000
    return when {
        min < 1 -> "az önce"
        min < 60 -> "$min dk önce"
        min < 1440 -> "${min / 60} sa önce"
        else -> "${min / 1440} gün önce"
    }
}
