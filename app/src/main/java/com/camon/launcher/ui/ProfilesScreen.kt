package com.camon.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.camon.launcher.core.ModLoader
import com.camon.launcher.core.PerformanceProfile
import com.camon.launcher.core.Profile
import com.camon.launcher.core.RendererType

@Composable
fun ProfilesScreen(vm: LauncherViewModel) {
    val profiles by vm.repository.profiles.collectAsStateCompat()
    val selectedId by vm.selectedProfileId.collectAsStateCompat()
    val selected = profiles.firstOrNull { it.id == selectedId }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Profile?>(null) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader("Profiller", "Her sürüm için ayrı mod, dünya ve Java ayarı")
        Button(onClick = { editing = null; showDialog = true }) {
            Text("+ Yeni profil")
        }
        Spacer(Modifier.height(8.dp))

        profiles.forEach { profile ->
            val isSelected = profile.id == selectedId
            Panel(
                title = (if (isSelected) "● " else "○ ") + profile.name,
                onClick = { vm.selectProfile(profile.id) }
            ) {
                Text(
                    "${profile.loader.display} ${profile.loaderVersion} • Minecraft ${profile.mcVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "RAM ${profile.ramMb} MB • Java ${profile.javaMajor} • ${profile.renderer.display} • ${profile.performance.display}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Oynama: ${formatDuration(profile.playTimeMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.selectProfile(profile.id); vm.launch() }) { Text("Başlat") }
                    TextButton(onClick = { editing = profile; showDialog = true }) { Text("Düzenle") }
                    TextButton(onClick = { vm.deleteProfile(profile.id) }) { Text("Sil") }
                }
            }
        }

        if (profiles.isEmpty()) EmptyState("Henüz profil yok.")
    }

    if (showDialog) {
        ProfileDialog(
            initial = editing,
            recommendedRam = vm.recommendedRam,
            onDismiss = { showDialog = false },
            onSave = { profile ->
                if (editing == null) vm.addProfile(profile)
                else vm.repository.updateProfile(profile)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ProfileDialog(
    initial: Profile?,
    recommendedRam: Int,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "Yeni profil") }
    var mcVersion by remember { mutableStateOf(initial?.mcVersion ?: "1.20.1") }
    var loader by remember { mutableStateOf(initial?.loader ?: ModLoader.FABRIC) }
    var renderer by remember { mutableStateOf(initial?.renderer ?: RendererType.OPENGL) }
    var performance by remember { mutableStateOf(initial?.performance ?: PerformanceProfile.BALANCED) }
    var ram by remember { mutableStateOf((initial?.ramMb ?: recommendedRam).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    (initial ?: Profile(name = name, mcVersion = mcVersion, loader = loader)).copy(
                        name = name,
                        mcVersion = mcVersion,
                        loader = loader,
                        loaderVersion = initial?.loaderVersion ?: "",
                        renderer = renderer,
                        performance = performance,
                        ramMb = ram.toInt(),
                        javaMajor = 17
                    )
                )
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        title = { Text(if (initial == null) "Yeni profil" else "Profili düzenle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ad") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mcVersion,
                    onValueChange = { mcVersion = it },
                    label = { Text("Minecraft sürümü") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Yükleyici", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModLoader.values().forEach { l ->
                        FilterChip(selected = loader == l, onClick = { loader = l }, label = { Text(l.display) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Renderer", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RendererType.values().forEach { r ->
                        FilterChip(selected = renderer == r, onClick = { renderer = r }, label = { Text(r.display) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Performans", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PerformanceProfile.values().forEach { p ->
                        FilterChip(selected = performance == p, onClick = { performance = p }, label = { Text(p.display) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("RAM: ${ram.toInt()} MB", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = ram,
                    onValueChange = { ram = it },
                    valueRange = 512f..6144f,
                    steps = 21
                )
                Text(
                    "Önerilen: $recommendedRam MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0 dk"
    val totalMin = ms / 60000
    val hours = totalMin / 60
    val minutes = totalMin % 60
    return if (hours > 0) "${hours} sa ${minutes} dk" else "${minutes} dk"
}
