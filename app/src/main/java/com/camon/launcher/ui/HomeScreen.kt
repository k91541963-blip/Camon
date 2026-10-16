package com.camon.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camon.launcher.core.Severity

@Composable
fun HomeScreen(vm: LauncherViewModel, onNavigate: (Dest) -> Unit) {
    val profiles by vm.repository.profiles.collectAsStateCompat()
    val live by vm.live.collectAsStateCompat()
    val selectedId by vm.selectedProfileId.collectAsStateCompat()
    val selected = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
    val device = vm.device
    val gpu = vm.gpu

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Camon", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text("Minecraft Java için akıllı başlatıcı", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Text(
                    selected?.let { "${it.name} • ${it.loader.display} ${it.mcVersion}" } ?: "Profil yok",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.launch() },
                        enabled = !live.running && selected != null
                    ) {
                        Text(if (live.running) "Çalışıyor…" else "Başlat")
                    }
                    OutlinedButton(onClick = { onNavigate(Dest.PROFILES) }) {
                        Text("Profiller")
                    }
                }
                if (live.running) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Canlı: ${live.fpsSamples.lastOrNull()?.toInt() ?: 0} FPS",
                        style = MaterialTheme.typography.titleMedium,
                        color = CamonColors.good
                    )
                    Spacer(Modifier.height(6.dp))
                    MiniLineChart(
                        samples = live.fpsSamples,
                        color = CamonColors.good,
                        maxOverride = selected?.performance?.targetFps?.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(70.dp)
                    )
                }
            }
        }

        SectionHeader("Cihaz", "${device.manufacturer} ${device.model}")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("RAM", "${device.totalRamMb} MB", CamonColors.info, Modifier.weight(1f))
            StatTile("Çekirdek", "${device.cores}", CamonColors.good, Modifier.weight(1f))
            StatTile("Android", device.androidVersion, CamonColors.warn, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("GPU", if (gpu.isMali) "Mali" else gpu.vendor.take(12), CamonColors.good, Modifier.weight(1f))
            StatTile("GLES", gpu.glesVersion.take(12), CamonColors.info, Modifier.weight(1f))
            StatTile("Vulkan", if (gpu.vulkanSupported) "Var" else "Yok", if (gpu.vulkanSupported) CamonColors.good else CamonColors.warn, Modifier.weight(1f))
        }

        SectionHeader("Akıllı öneriler")
        vm.recommendations.forEach { rec ->
            Panel(title = rec.title) {
                Row {
                    SmallDot(severityColor(rec.severity))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        rec.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionHeader("Hızlı erişim")
        val quick = listOf(
            Dest.RENDERERS, Dest.JAVA_RAM, Dest.MODS, Dest.PERFORMANCE,
            Dest.CRASH, Dest.MALI, Dest.ACCOUNTS, Dest.SETTINGS
        )
        quick.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { dest ->
                    Panel(
                        title = "${dest.glyph}  ${dest.title}",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(dest) }
                    ) {
                        Text(
                            destHint(dest),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Panel(title = "Oyun çalıştırma çekirdeği") {
            Text(
                "Bu sürüm yönetim, cihaz analizi ve performans katmanını içerir. " +
                    "Minecraft Java'yı gerçekten başlatmak için Zalith/Pojav tabanlı yerel çalıştırma " +
                    "çekirdeğinin (JVM + LWJGL köprüsü) projeye eklenmesi gerekir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun destHint(dest: Dest): String = when (dest) {
    Dest.RENDERERS -> "OpenGL / Vulkan seçimi"
    Dest.JAVA_RAM -> "Bel(lek) ve Java sürümü"
    Dest.MODS -> "Etkinleştir, bağımlılık denetle"
    Dest.PERFORMANCE -> "FPS ve kare süreleri"
    Dest.CRASH -> "Günlükten neden bul"
    Dest.MALI -> "Mali'ye özel öneriler"
    Dest.ACCOUNTS -> "Microsoft / çevrimdışı"
    Dest.SETTINGS -> "Tema, animasyon, arayüz"
    else -> ""
}
