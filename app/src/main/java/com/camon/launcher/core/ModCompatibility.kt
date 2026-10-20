package com.camon.launcher.core

object ModCompatibility {

    fun check(mods: List<ModItem>, profile: Profile): List<ModIssue> {
        val issues = mutableListOf<ModIssue>()
        val namesLower = mods.map { it.name.lowercase() }

        for (mod in mods) {
            if (mod.loader != profile.loader && mod.loader != ModLoader.VANILLA) {
                issues += ModIssue(
                    mod.name,
                    IssueKind.WRONG_LOADER,
                    "Mod ${mod.loader.display} için; profil ${profile.loader.display} kullanıyor."
                )
            }

            if (mod.mcVersions.isNotEmpty() && profile.mcVersion !in mod.mcVersions) {
                issues += ModIssue(
                    mod.name,
                    IssueKind.VERSION_MISMATCH,
                    "Mod ${mod.mcVersions.joinToString(", ")} sürümlerini destekliyor, profil " +
                        "${profile.mcVersion}."
                )
            }

            for (dep in mod.dependencies) {
                val present = namesLower.any { it == dep.lowercase() }
                if (!present) {
                    issues += ModIssue(
                        mod.name,
                        IssueKind.MISSING_DEPENDENCY,
                        "'$dep' bağımlılığı kurulu değil."
                    )
                }
            }
        }

        // Heuristic conflicts: known pairs that fight over the same feature.
        val conflictGroups = listOf(
            listOf("optifine", "sodium"),
            listOf("sodium", "rubidium"),
            listOf("iris", "optifine"),
            listOf("fabric-api", "quilted-fabric-api"),
            listOf("webdisplays", "optifine"),
            listOf("webdisplays", "sodium"),
            listOf("webdisplays", "iris")
        )
        for (group in conflictGroups) {
            val present = group.filter { g -> namesLower.any { it.contains(g) } }
            if (present.size >= 2) {
                issues += ModIssue(
                    present.joinToString(" + "),
                    IssueKind.CONFLICT,
                    "Bu modlar aynı işlevi paylaşıyor; birlikte kararsızlık yaratabilir."
                )
            }
        }

        return issues
    }

    fun summary(issues: List<ModIssue>): String {
        if (issues.isEmpty()) return "Tüm modlar uyumlu görünüyor."
        val byKind = issues.groupingBy { it.kind }.eachCount()
        return byKind.entries.joinToString("  •  ") { "${it.key.display}: ${it.value}" }
    }

    // ---- Android özel uyumluluk uyarıları ----
    private val androidProblematic = listOf(
        PlatformModIssue(
            modName = "webdisplays",
            platform = "android",
            reason = "WebDisplays MCEF (Chromium) kütüphanesine bağımlıdır; MCEF sadece masaüstü x86_64 için derlenmiş olup Android/ARM'de çalışmaz. Beyaz ekran veya kütüphane yükleme hatası verir.",
            severity = Severity.DANGER
        ),
        PlatformModIssue(
            modName = "veil",
            platform = "android",
            reason = "imgui-java yerel kitaplığı sadece x86_64 natives içerir; ARM64 Android'de UnsatisfiedLinkError/SIGSEGV ile çöker.",
            severity = Severity.DANGER
        ),
        PlatformModIssue(
            modName = "sable",
            platform = "android",
            reason = "Veil'e bağımlıdır; aynı x86_64 native kütüphane sorununu taşır.",
            severity = Severity.DANGER
        ),
        PlatformModIssue(
            modName = "optifine",
            platform = "android",
            reason = "Diğer modlarla uyumsuz davranabilir ve Android renderer'da çökme yapabilir; Embeddium/Rubidium tercih edin.",
            severity = Severity.WARN
        ),
        PlatformModIssue(
            modName = "distant horizons",
            platform = "android",
            reason = "ARM64 SQL yerel kitaplıkları doğru yüklenemeyebilir ve çökmeye neden olur.",
            severity = Severity.WARN
        )
    )

    fun androidProblematicMods(mods: List<ModItem>): List<PlatformModIssue> {
        val namesLower = mods.map { it.name.lowercase() }
        return androidProblematic.filter { issue ->
            namesLower.any { it.contains(issue.modName) }
        }
    }

    fun androidSummary(issues: List<PlatformModIssue>): String {
        if (issues.isEmpty()) return "Android'de bilinen sorunlu mod bulunamadı."
        return issues.joinToString("  •  ") { "${it.modName}: ${it.reason}" }
    }
}
