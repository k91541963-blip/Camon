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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.camon.launcher.core.PerformanceProfile

@Composable
fun PerformanceScreen(vm: LauncherViewModel) {
    val live by vm.live.collectAsStateCompat()
    val history by vm.repository.history.collectAsStateCompat()
    val profiles by vm.repository.profiles.collectAsStateCompat()
    val selectedId by vm.selectedProfileId.collectAsStateCompat()
    val selected = profiles.firstOrNull { it.id == selectedId }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Canlı Performans Paneli", "FPS, kare süresi ve bellek")

        Panel(title = if (live.running) "Ölçüm sürüyor • ${live.profileName}" else "Beklemede") {
            val currentFps = live.fpsSamples.lastOrNull()?.toInt() ?: 0
            val frameMs = live.frameMsSamples.lastOrNull() ?: 0f
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("FPS", "$currentFps", CamonColors.good, Modifier.weight(1f))
                StatTile("Kare", "%.1f ms".format(frameMs), CamonColors.info, Modifier.weight(1f))
                StatTile("Ort.", "${live.fpsSamples.average().takeIf { !it.isNaN() }?.toInt() ?: 0}", CamonColors.warn, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            MiniLineChart(
                samples = live.fpsSamples,
                color = CamonColors.good,
                maxOverride = selected?.performance?.targetFps?.toFloat(),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "RAM kullanımı: ${vm.device.availRamMb} / ${vm.device.totalRamMb} MB boş",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Sıcaklık: cihaz destekliyorsa sistem servisinden okunur.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.launch() }, enabled = !live.running && selected != null) {
                Text(if (live.running) "Ölçüm sürüyor…" else "Ölçümü başlat")
            }
        }

        if (selected != null) {
            Panel(title = "Performans profili: ${selected.name}") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PerformanceProfile.values().forEach { p ->
                        FilterChip(
                            selected = selected.performance == p,
                            onClick = { vm.repository.updateProfile(selected.copy(performance = p)) },
                            label = { Text(p.display) }
                        )
                    }
                }
                Text(
                    "Hedef ${selected.performance.targetFps} FPS • ${selected.performance.renderDistance} chunk",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionHeader("Performans geçmişi")
        if (history.isEmpty()) {
            EmptyState("Kayıtlı oturum yok.")
        } else {
            val fps = history.take(20).reversed().map { it.avgFps.toFloat() }
            Panel(title = "Ortalama FPS eğilimi") {
                MiniLineChart(samples = fps, color = CamonColors.info, modifier = Modifier.fillMaxWidth().height(90.dp))
            }
        }
    }
}

@Composable
fun CrashScreen(vm: LauncherViewModel) {
    val reports by vm.repository.crashReports.collectAsStateCompat()
    val last by vm.lastCrash.collectAsStateCompat()
    var logText by remember { mutableStateOf("") }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Akıllı Çökme Analizi", "Günlükten olası nedeni bul")
        Panel(title = "Günlük yapıştır") {
            OutlinedTextField(
                value = logText,
                onValueChange = { logText = it },
                label = { Text("latest.log / crash-report metni") },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.analyzeLogText(logText) }, enabled = logText.isNotBlank()) { Text("Analiz et") }
        }

        last?.let { report ->
            Panel(title = "Son analiz: " + report.title) {
                Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Öneriler", style = MaterialTheme.typography.titleMedium)
                report.causes.forEach { cause ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        SmallDot(CamonColors.warn)
                        Spacer(Modifier.height(0.dp))
                        Text("  $cause", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        SectionHeader("Kayıtlı raporlar")
        reports.forEach { report ->
            Panel(title = report.title) {
                Text("${report.profileName} • ${formatTimestamp(report.timestamp)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                if (report.logExcerpt.isNotBlank()) {
                    Text(
                        report.logExcerpt.take(400),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { vm.repository.removeCrashReport(report.id) }) { Text("Sil") }
            }
        }
        if (reports.isEmpty()) EmptyState("Çökme raporu yok.")
    }
}

@Composable
fun HistoryScreen(vm: LauncherViewModel) {
    val history by vm.repository.history.collectAsStateCompat()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        SectionHeader("Oturum Geçmişi", "Oynama süresi, FPS ve çökme kayıtları")

        if (history.size >= 2) {
            val recent = history.take(2)
            val a = recent[0]
            val b = recent[1]
            Panel(title = "Karşılaştırma (son iki oturum)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(a.profileName, style = MaterialTheme.typography.labelSmall, color = CamonColors.info)
                    Text(b.profileName, style = MaterialTheme.typography.labelSmall, color = CamonColors.warn)
                }
                InfoRow("Ort. FPS", "${a.avgFps}  →  ${b.avgFps}")
                InfoRow("En düşük", "${a.minFps}  →  ${b.minFps}")
                InfoRow("Süre", "${formatDuration(a.durationMs)}  →  ${formatDuration(b.durationMs)}")
                val delta = a.avgFps - b.avgFps
                Text(
                    if (delta >= 0) "Son oturum %d FPS daha iyi.".format(delta)
                    else "Önceki oturum %d FPS daha iyi.".format(-delta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (delta >= 0) CamonColors.good else CamonColors.warn,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        history.forEach { session ->
            Panel(title = session.profileName) {
                Text(formatTimestamp(session.start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                InfoRow("Süre", formatDuration(session.durationMs))
                InfoRow("FPS (min/ort/maks)", "${session.minFps} / ${session.avgFps} / ${session.maxFps}")
                Pill(if (session.crashed) "Çöktü" else "Tamamlandı", if (session.crashed) CamonColors.bad else CamonColors.good)
            }
        }
        if (history.isEmpty()) EmptyState("Oturum kaydı yok.")
    }
}
