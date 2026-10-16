package com.camon.launcher.core

object CrashAnalyzer {

    private data class Rule(
        val keywords: List<String>,
        val title: String,
        val summary: String,
        val cause: String
    )

    private val rules = listOf(
        Rule(
            listOf("outofmemoryerror", "java heap space", "could not reserve enough space"),
            "Bellek yetersiz",
            "Oyun çalışırken Java belleği tükendi.",
            "Profilden RAM miktarını artır veya daha hafif bir mod paketi kullan."
        ),
        Rule(
            listOf("nosuchmethoderror", "noclassdeffounderror", "classnotfoundexception"),
            "Kütüphane / sürüm uyuşmazlığı",
            "Bir mod veya kütüphane beklenen sınıfı bulamadı.",
            "Modu Minecraft sürümüne ve yükleyici sürümüne uygun güncelle. Eksik bağımlılıkları kur."
        ),
        Rule(
            listOf("unsupportedclassversionerror"),
            "Yanlış Java sürümü",
            "Mod veya derlenmiş dosya daha yeni bir Java ile üretilmiş.",
            "1.17+ için Java 21, 1.16 ve altı için Java 8/17 seç."
        ),
        Rule(
            listOf("lwjgl", "egl_bad_", "glfw error", "opengl", "vulkan", "failed to create window"),
            "Grafik / renderer hatası",
            "Renderer penceresi oluşturulamadı ya da GPU sürücüsü hata verdi.",
            "Farklı bir renderer dene, shader'ları kapat ve görüş mesafesini düşür."
        ),
        Rule(
            listOf("mixin apply", "mixin", "mixinconfig"),
            "Mixin çakışması",
            "İki veya daha fazla mod aynı oyun kodunu değiştirmeye çalıştı.",
            "Çakışan modlardan birini devre dışı bırak; uyumluluk sürümlerini kontrol et."
        ),
        Rule(
            listOf("duplicate mod", "mod file", "already loaded", "conflicting mod"),
            "Mod çakışması",
            "Aynı işlevi gören iki mod yüklendi.",
            "Yinelenen modlardan birini sil veya devre dışı bırak."
        ),
        Rule(
            listOf("unable to access", "permission denied", "read-only file system"),
            "Dosya izni sorunu",
            "Oyun bir dosyaya erişemedi.",
            "Depolama iznini ver ve oyun klasörünü kontrol et."
        ),
        Rule(
            listOf("modid", "requires", "missing dependency", "depends on"),
            "Eksik mod bağımlılığı",
            "Bir mod gerekli başka bir modu bulamadı.",
            "Eksik bağımlılık modunu aynı sürümle kur."
        ),
        Rule(
            listOf("no such file", "filenotfoundexception"),
            "Eksik dosya",
            "Beklenen bir oyun veya mod dosyası eksik.",
            "Dosya bütünlüğünü doğrula ve ilgili modu yeniden kur."
        ),
        Rule(
            listOf("authentication", "invalid session", "session expired", "401", "403"),
            "Oturum hatası",
            "Microsoft oturumu geçersiz veya süresi dolmuş.",
            "Hesapları yeniden doğrula."
        )
    )

    fun analyze(profile: Profile, logText: String): CrashReport {
        val lower = logText.lowercase()
        val matched = rules.filter { rule -> rule.keywords.any { lower.contains(it) } }

        val title: String
        val summary: String
        val causes: List<String>

        if (matched.isEmpty()) {
            title = "Belirgin neden bulunamadı"
            summary = "Günlük dosyasında bilinen hata kalıpları eşleşmedi."
            causes = listOf(
                "Günlüğün tamamını kaydet ve daha ayrıntılı bir çalıştırma dene.",
                "Java sürümü ile Minecraft sürümünün uyumlu olduğundan emin ol."
            )
        } else {
            title = matched.first().title
            summary = matched.joinToString(" ") { it.summary }
            causes = matched.map { it.cause }.distinct()
        }

        val excerpt = extractRelevant(logText)

        return CrashReport(
            profileId = profile.id,
            profileName = profile.name,
            title = title,
            summary = summary,
            causes = causes,
            logExcerpt = excerpt
        )
    }

    private fun extractRelevant(logText: String): String {
        val lines = logText.lines()
        val errorLines = lines.filter { line ->
            val l = line.lowercase()
            l.contains("error") || l.contains("exception") || l.contains("caused by") ||
                l.contains("fatal") || l.contains("crash")
        }
        val chosen = if (errorLines.isNotEmpty()) errorLines.take(20) else lines.takeLast(20)
        return chosen.joinToString("\n").take(4000)
    }
}
