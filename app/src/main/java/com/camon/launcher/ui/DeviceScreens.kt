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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camon.launcher.core.RendererType
import com.camon.launcher.core.Severity

@Composable
fun RendererScreen(vm: LauncherViewModel) {
    val profiles by vm.repository.profiles.collectAsStateCompat()
    val selectedId by vm.selectedProfileId.collectAsStateCompat()
    val selected = profiles.firstOrNull { it.id == selectedId }
    val options = vm.rendererOptions()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Renderer Yöneticisi", "Grafik motorlarını listele ve profil bazında seç")
        options.forEach { option ->
            Panel(title = option.name + if (option.recommended) "  ★ önerilen" else "") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        if (option.supported) "Destekleniyor" else "Desteklenmiyor",
                        if (option.supported) CamonColors.good else CamonColors.bad
                    )
                    if (selected != null) {
                        Pill(
                            if (selected.renderer == option.type) "Seçili" else "Seç",
                            if (selected.renderer == option.type) CamonColors.info else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(option.maliNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selected != null && option.supported) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.repository.updateProfile(selected.copy(renderer = option.type)) }) {
                        Text("Bu profile uygula")
                    }
                }
            }
        }
    }
}

@Composable
fun JavaRamScreen(vm: LauncherViewModel) {
    val profiles by vm.repository.profiles.collectAsStateCompat()
    val selectedId by vm.selectedProfileId.collectAsStateCompat()
    val selected = profiles.firstOrNull { it.id == selectedId }
    val device = vm.device
    val runtimes = vm.javaRuntimes()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Java & RAM Yöneticisi", "Sürüme uygun Java ve bellek ayarı")

        Panel(title = "Cihaz belleği") {
            BarRow("Kullanılabilir", device.availRamMb.toFloat() / device.totalRamMb, "${device.availRamMb} MB", CamonColors.info)
            InfoRow("Toplam RAM", "${device.totalRamMb} MB")
            InfoRow("Önerilen Minecraft RAM", "${vm.recommendedRam} MB")
            if (device.availRamMb < 900) {
                Spacer(Modifier.height(6.dp))
                Text("Bel(lek) baskısı yüksek; arka plan uygulamalarını kapat.", color = CamonColors.warn)
            }
        }

        Panel(title = "Java ortamları") {
            runtimes.forEach { rt ->
                val isRecommended = selected != null && rt.major == vm.recommendedJava(selected.mcVersion)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(rt.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(rt.path + " • " + rt.arch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isRecommended) Pill("önerilen", CamonColors.good)
                }
            }
        }

        if (selected != null) {
            var ram by remember(selected.id) { mutableStateOf(selected.ramMb.toFloat()) }
            Panel(title = "Profil belleği: ${selected.name}") {
                Text("RAM: ${ram.toInt()} MB", style = MaterialTheme.typography.bodyMedium)
                Slider(value = ram, onValueChange = { ram = it }, valueRange = 512f..6144f, steps = 21)
                val tooHigh = ram.toInt() > device.totalRamMb * 0.6f
                val tooLow = ram.toInt() < 1024
                if (tooHigh) Text("Uyarı: toplam RAM'in %60'ından fazlası; cihaz takılabilir.", color = CamonColors.warn)
                if (tooLow) Text("Uyarı: 1024 MB altı bazı mod paketlerinde çökme yapar.", color = CamonColors.warn)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.repository.updateProfile(selected.copy(ramMb = ram.toInt())) }) {
                    Text("Kaydet")
                }
            }
        }
    }
}

@Composable
fun MaliScreen(vm: LauncherViewModel) {
    val gpu = vm.gpu
    val device = vm.device
    val shaders by vm.repository.shaders.collectAsStateCompat()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Mali GPU", if (gpu.isMali) "Mali algılandı" else "Mali algılanmadı (özellikler genel)")

        Panel(title = "Algılama") {
            InfoRow("Üretici", gpu.vendor)
            InfoRow("Model", gpu.model)
            InfoRow("Sürücü", gpu.driverVersion)
            InfoRow("GLES", gpu.glesVersion)
            InfoRow("Vulkan", if (gpu.vulkanSupported) "Destekleniyor" else "Desteklenmiyor")
            InfoRow("Maks doku", "${gpu.maxTextureSize} px")
        }

        Panel(title = "Grafik API uyumluluk testi") {
            Text(
                if (gpu.vulkanSupported)
                    "Vulkan bildiriliyor ancak Mali sürücülerinde kararsız olabilir. Önce OpenGL ES dene."
                else "Vulkan yok. OpenGL ES renderer'ında kal; Zink/Vulkan denemesi başarısız olur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Panel(title = "Shader uyumluluğu") {
            if (shaders.isEmpty()) {
                Text("Kurulu shader yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                shaders.forEach { shader ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(shader.name, style = MaterialTheme.typography.bodyMedium)
                        val ok = shader.compatibleWithMali || !gpu.isMali
                        Pill(if (ok) "uyumlu" else "risk", if (ok) CamonColors.good else CamonColors.warn)
                    }
                    if (shader.requiresFeatures.isNotEmpty()) {
                        Text(
                            "Gereksinim: " + shader.requiresFeatures.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Panel(title = "Chunk yükleme optimizasyonu") {
            val profile = vm.selectedProfile()
            val dist = profile?.performance?.renderDistance ?: 10
            Text(
                "Görüş mesafesi $dist chunk. Mali GPU'larda büyük render mesafesi takılma yaratır; " +
                    "uçuş sırasında ${(dist - 2).coerceAtLeast(4)} chunk deneyin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Panel(title = "Isınma ve performans takibi") {
            Text(
                "Cihaz sıcaklığı android tarafından her zaman açılmaz. " +
                    "Uzun oturumlarda FPS düşüşü ısınma göstergesidir; " +
                    "gölge ve shader ayarlarını kısmak kalıcı çözümdür.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Panel(title = "Karşılaştırma ve test") {
            Text(
                "Farklı renderer ve profil ayarlarıyla oturum kaydedip " +
                    "Oturum Geçmişi'nde ortalama FPS ve düşük FPS değerlerini karşılaştırabilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            InfoRow("Çekirdek", "${device.cores}")
            InfoRow("SoC", device.socModel)
        }
    }
}

@Composable
fun ControlsScreen(vm: LauncherViewModel) {
    val controls by vm.repository.touchControls.collectAsStateCompat()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Dokunmatik Kontroller", "Konum, boyut, saydamlık ve görünürlük")
        Button(onClick = { showAdd = true }) { Text("+ Kontrol ekle") }
        Spacer(Modifier.height(8.dp))

        controls.forEach { control ->
            Panel(title = control.label) {
                Text("Konum: %.2f, %.2f".format(control.x, control.y), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Boyut: %.2fx   Saydamlık: %.2f".format(control.size, control.alpha), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Boyut", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = control.size,
                    onValueChange = { vm.repository.updateTouchControl(control.copy(size = it)) },
                    valueRange = 0.5f..2f
                )
                Text("Saydamlık", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = control.alpha,
                    onValueChange = { vm.repository.updateTouchControl(control.copy(alpha = it)) },
                    valueRange = 0.2f..1f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Görünür", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = control.visible,
                        onCheckedChange = { vm.repository.updateTouchControl(control.copy(visible = it)) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        var label by remember { mutableStateOf("Yeni tuş") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = { vm.addTouchControl(label); showAdd = false }) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } },
            title = { Text("Kontrol ekle") },
            text = {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Etiket") })
            }
        )
    }
}

@Composable
fun GamepadScreen(vm: LauncherViewModel) {
    val gamepads by vm.repository.gamepads.collectAsStateCompat()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Gamepad Profilleri", "Oyun kolu tuş eşlemeleri")
        Button(onClick = { showAdd = true }) { Text("+ Profil ekle") }
        Spacer(Modifier.height(8.dp))

        gamepads.forEach { pad ->
            Panel(title = pad.name) {
                Text(pad.deviceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                pad.bindings.forEach { (button, action) ->
                    InfoRow(button, action)
                }
                TextButton(onClick = { vm.repository.removeGamepad(pad.id) }) { Text("Sil") }
            }
        }
        if (gamepads.isEmpty()) EmptyState("Gamepad profili yok.")
    }

    if (showAdd) {
        var name by remember { mutableStateOf("Yeni gamepad") }
        var deviceName by remember { mutableStateOf("Bluetooth Gamepad") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = { vm.addGamepad(name, deviceName); showAdd = false }) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } },
            title = { Text("Gamepad profili ekle") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profil adı") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = deviceName, onValueChange = { deviceName = it }, label = { Text("Cihaz adı") })
                }
            }
        )
    }
}
