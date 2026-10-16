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
import androidx.compose.material3.Slider
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
import com.camon.launcher.core.AccountType
import com.camon.launcher.core.ModLoader
import com.camon.launcher.core.Profile
import com.camon.launcher.core.RendererType

@Composable
fun AccountsScreen(vm: LauncherViewModel) {
    val accounts by vm.repository.accounts.collectAsStateCompat()
    var showOffline by remember { mutableStateOf(false) }
    var showMicrosoft by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Hesaplar", "Güvenli oturum yönetimi")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showMicrosoft = true }) { Text("Microsoft ekle") }
            OutlinedButton(onClick = { showOffline = true }) { Text("Çevrimdışı ekle") }
        }
        Spacer(Modifier.height(10.dp))

        Panel(title = "Güvenlik") {
            Text(
                "Microsoft oturumu resmî OAuth cihaz kodu akışıyla yürütülür; " +
                    "parolan bu uygulamada toplanmaz ve saklanmaz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        accounts.forEach { account ->
            Panel(title = (if (account.active) "● " else "○ ") + account.username) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(account.type.display, if (account.type == AccountType.MICROSOFT) CamonColors.info else CamonColors.warn)
                }
                if (account.uuid.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(account.uuid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!account.active) {
                        TextButton(onClick = { vm.repository.setActiveAccount(account.id) }) { Text("Etkinleştir") }
                    }
                    TextButton(onClick = { vm.repository.removeAccount(account.id) }) { Text("Sil") }
                }
            }
        }
        if (accounts.isEmpty()) EmptyState("Hesap yok.")
    }

    if (showOffline) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOffline = false },
            confirmButton = { TextButton(onClick = { vm.addOfflineAccount(name); showOffline = false }) { Text("Ekle") } },
            dismissButton = { TextButton(onClick = { showOffline = false }) { Text("İptal") } },
            title = { Text("Çevrimdışı hesap") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Kullanıcı adı") }) }
        )
    }
    if (showMicrosoft) {
        var email by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMicrosoft = false },
            confirmButton = { TextButton(onClick = { vm.addMicrosoftAccount(email); showMicrosoft = false }) { Text("Devam") } },
            dismissButton = { TextButton(onClick = { showMicrosoft = false }) { Text("İptal") } },
            title = { Text("Microsoft hesabı") },
            text = {
                Column {
                    Text("Cihaz kodu akışı başlatılacak. E-posta gir; ardından tarayıcıdan kodu onaylaman istenir.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") })
                }
            }
        )
    }
}

@Composable
fun WizardScreen(vm: LauncherViewModel) {
    var step by remember { mutableStateOf(0) }
    val device = vm.device
    val gpu = vm.gpu
    val java = vm.recommendedJava("1.20.1")
    val renderer = if (gpu.isMali || !gpu.vulkanSupported) RendererType.OPENGL else RendererType.VULKAN

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("İlk Kurulum Sihirbazı", "Adım ${step + 1} / 4")

        when (step) {
            0 -> Panel(title = "1. Cihaz kontrolü") {
                InfoRow("Cihaz", "${device.manufacturer} ${device.model}")
                InfoRow("RAM", "${device.totalRamMb} MB")
                InfoRow("Çekirdek", "${device.cores}")
                InfoRow("GPU", if (gpu.isMali) "Mali • ${gpu.model}" else gpu.model)
                InfoRow("Vulkan", if (gpu.vulkanSupported) "var" else "yok")
            }
            1 -> Panel(title = "2. Java ve bellek önerisi") {
                InfoRow("Önerilen Java", "Java $java")
                InfoRow("Önerilen RAM", "${vm.recommendedRam} MB")
                Text(
                    "Bu değerler Minecraft 1.20.1 için hesaplandı; profili oluşturunca değiştirebilirsin.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            2 -> Panel(title = "3. Renderer ve performans") {
                InfoRow("Önerilen renderer", renderer.display)
                InfoRow("Performans profili", vm.recommendedPerformance.display)
                InfoRow("Hedef FPS", "${vm.recommendedPerformance.targetFps}")
                InfoRow("Görüş mesafesi", "${vm.recommendedPerformance.renderDistance} chunk")
            }
            3 -> Panel(title = "4. Hazır") {
                Text(
                    "Camon hazır. Aşağıdaki düğme cihazına uygun bir profil oluşturur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }) { Text("Geri") }
            }
            if (step < 3) {
                Button(onClick = { step++ }) { Text("İleri") }
            } else {
                Button(onClick = {
                    vm.addProfile(
                        Profile(
                            name = "Önerilen profil",
                            mcVersion = "1.20.1",
                            loader = ModLoader.FABRIC,
                            loaderVersion = "0.15.11",
                            ramMb = vm.recommendedRam,
                            javaMajor = java,
                            renderer = renderer,
                            performance = vm.recommendedPerformance
                        )
                    )
                    vm.updateSettings { it.copy(wizardCompleted = true) }
                }) { Text("Profili oluştur ve bitir") }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val settings by vm.repository.settings.collectAsStateCompat()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Ayarlar", "Tema, arayüz ve davranış")

        Panel(title = "Görünüm") {
            ToggleRow("Karanlık tema", settings.darkTheme) { value ->
                vm.updateSettings { it.copy(darkTheme = value) }
            }
            ToggleRow("Animasyonlar", settings.animations) { value ->
                vm.updateSettings { it.copy(animations = value) }
            }
            ToggleRow("Launcher'ı hafif tut", settings.lightModeLauncher) { value ->
                vm.updateSettings { it.copy(lightModeLauncher = value) }
            }
            ToggleRow("Performans göstergesi", settings.performanceOverlay) { value ->
                vm.updateSettings { it.copy(performanceOverlay = value) }
            }
        }

        Panel(title = "Yedekleme") {
            ToggleRow("Otomatik yedekleme", settings.autoBackup) { value ->
                vm.updateSettings { it.copy(autoBackup = value) }
            }
            Text("Aralık: ${settings.backupIntervalHours} saat", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.backupIntervalHours.toFloat(),
                onValueChange = { v -> vm.updateSettings { it.copy(backupIntervalHours = v.toInt()) } },
                valueRange = 1f..72f,
                steps = 70
            )
        }

        Panel(title = "İndirmeler") {
            Text("Eşzamanlı indirme: ${settings.downloadConcurrency}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.downloadConcurrency.toFloat(),
                onValueChange = { v -> vm.updateSettings { it.copy(downloadConcurrency = v.toInt().coerceAtLeast(1)) } },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        Panel(title = "Hakkında") {
            InfoRow("Camon", "Sürüm 1.0.0")
            InfoRow("Arayüz", "Kotlin + Jetpack Compose")
            InfoRow("Lisans", "GPL-3.0 (Zalith tabanlı)")
            Text(
                "Zalith Launcher açık kaynak kodundan esinlenmiştir. Kaynak kodu değiştirilip " +
                    "dağıtılırsa GPL-3.0 koşulları korunmalıdır.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
