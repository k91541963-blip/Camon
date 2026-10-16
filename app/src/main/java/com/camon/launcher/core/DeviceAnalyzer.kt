package com.camon.launcher.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.os.Build
import java.io.File

object DeviceAnalyzer {

    fun analyzeDevice(context: Context): DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val totalMb = (mi.totalMem / (1024L * 1024L)).toInt()
        val availMb = (mi.availMem / (1024L * 1024L)).toInt()

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "?",
            model = Build.MODEL ?: "?",
            androidVersion = Build.VERSION.RELEASE ?: "?",
            sdkInt = Build.VERSION.SDK_INT,
            cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "?",
            cores = Runtime.getRuntime().availableProcessors(),
            cpuMaxMhz = readCpuMaxMhz(),
            totalRamMb = totalMb,
            availRamMb = availMb,
            lowRamDevice = am.isLowRamDevice,
            socModel = readSocModel()
        )
    }

    private fun readSocModel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Build.SOC_MODEL.isNullOrBlank()) {
            Build.SOC_MODEL
        } else {
            (Build.HARDWARE ?: "?") + " / " + (Build.BOARD ?: "?")
        }
    }

    private fun readCpuMaxMhz(): Int {
        val candidates = listOf(
            "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq",
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
        )
        for (path in candidates) {
            try {
                val f = File(path)
                if (f.canRead()) {
                    val khz = f.readText().trim().toLongOrNull() ?: continue
                    if (khz > 0) return (khz / 1000L).toInt()
                }
            } catch (_: Exception) {
            }
        }
        return 0
    }

    fun analyzeGpu(context: Context): GpuInfo {
        val renderer = try {
            GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
        } catch (_: Exception) {
            ""
        } catch (_: Throwable) {
            ""
        }
        val vendor = try {
            GLES20.glGetString(GLES20.GL_VENDOR) ?: ""
        } catch (_: Throwable) {
            ""
        }
        val version = try {
            GLES20.glGetString(GLES20.GL_VERSION) ?: ""
        } catch (_: Throwable) {
            ""
        }
        val maxTex = try {
            val arr = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, arr, 0)
            arr[0]
        } catch (_: Throwable) {
            0
        }

        val combined = (renderer + " " + vendor).lowercase()
        val isMali = combined.contains("mali")
        val vulkanVersion = readVulkanVersion(context)

        return GpuInfo(
            vendor = vendor.ifBlank { "Bilinmiyor" },
            model = renderer.ifBlank { "Bilinmiyor" },
            isMali = isMali,
            glesVersion = extractGles(version),
            vulkanSupported = vulkanVersion > 0,
            driverVersion = detectDriver(renderer, vendor),
            maxTextureSize = maxTex
        )
    }

    private fun extractGles(version: String): String {
        val idx = version.indexOfFirst { it.isDigit() }
        return if (idx >= 0) version.substring(idx).take(20) else version.ifBlank { "bilinmiyor" }
    }

    private fun readVulkanVersion(context: Context): Int {
        return try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
                context.packageManager.systemAvailableFeatures
                    .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                    ?.version ?: 0
            } else 0
        } catch (_: Exception) {
            0
        }
    }

    private fun detectDriver(renderer: String, vendor: String): String {
        val r = renderer.lowercase()
        return when {
            r.contains("mali") -> "ARM Mali sürücüsü"
            r.contains("adreno") -> "Qualcomm Adreno"
            r.contains("powervr") -> "Imagination PowerVR"
            r.contains("tegra") -> "NVIDIA Tegra"
            vendor.isBlank() -> "Bilinmiyor"
            else -> vendor
        }
    }

    fun recommendRam(device: DeviceInfo): Int {
        val safeBudget = when {
            device.lowRamDevice -> device.totalRamMb / 5
            device.totalRamMb >= 12_000 -> device.totalRamMb / 4
            device.totalRamMb >= 8_000 -> device.totalRamMb / 4
            device.totalRamMb >= 6_000 -> device.totalRamMb / 5
            else -> device.totalRamMb / 5
        }
        val rounded = (safeBudget / 256) * 256
        return rounded.coerceIn(1024, 6144)
    }

    fun recommendPerformanceProfile(device: DeviceInfo, gpu: GpuInfo): PerformanceProfile {
        return when {
            device.lowRamDevice || device.cores <= 4 -> PerformanceProfile.POWER_SAVE
            gpu.isMali -> PerformanceProfile.BALANCED
            device.totalRamMb >= 8_000 && device.cores >= 8 -> PerformanceProfile.PERFORMANCE
            else -> PerformanceProfile.BALANCED
        }
    }

    fun recommendations(device: DeviceInfo, gpu: GpuInfo): List<Recommendation> {
        val list = mutableListOf<Recommendation>()

        val recommendedRam = recommendRam(device)
        list += Recommendation(
            "Önerilen RAM: ${recommendedRam} MB",
            "Toplam ${device.totalRamMb} MB / kullanılabilir ${device.availRamMb} MB. " +
                "Minecraft'a bu kadar bırakmak sistemi rahatlatır.",
            Severity.INFO
        )

        if (device.availRamMb < 900) {
            list += Recommendation(
                "Bellek sıkıntısı",
                "Boşta kalan RAM ${device.availRamMb} MB. Oyun açılmadan önce arka plan uygulamalarını kapat.",
                Severity.WARN
            )
        }

        if (device.lowRamDevice) {
            list += Recommendation(
                "Düşük RAM'li cihaz",
                "Kısa görüş mesafesi ve güç tasarrufu profili öneriliyor. Shader kullanmaktan kaçın.",
                Severity.WARN
            )
        }

        if (gpu.isMali) {
            list += Recommendation(
                "Mali GPU algılandı",
                "Mali sürücülerinde Vulkan bazı modellerde kararsız olabilir. Önce OpenGL ES dene, " +
                    "kararlıysa Vulkan'a geç.",
                Severity.INFO
            )
        }

        if (!gpu.vulkanSupported) {
            list += Recommendation(
                "Vulkan yok",
                "Bu cihaz Vulkan bildirmiyor. OpenGL ES renderer'ında kal.",
                Severity.WARN
            )
        }

        val profile = recommendPerformanceProfile(device, gpu)
        list += Recommendation(
            "Önerilen profil: ${profile.display}",
            "${profile.targetFps} FPS hedefi, ${profile.renderDistance} chunk görüş mesafesi.",
            Severity.OK
        )

        if (device.cores <= 4) {
            list += Recommendation(
                "Sınırlı CPU çekirdeği",
                "${device.cores} çekirdek. Chunk oluşturmayı yavaşlatmak takılmaları azaltır.",
                Severity.INFO
            )
        }

        return list
    }
}
