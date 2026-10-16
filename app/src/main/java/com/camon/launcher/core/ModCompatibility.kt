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
            listOf("fabric-api", "quilted-fabric-api")
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
}
