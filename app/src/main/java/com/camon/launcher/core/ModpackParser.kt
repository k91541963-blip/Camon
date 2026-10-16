package com.camon.launcher.core

import org.json.JSONObject

data class ModpackFile(
    val path: String,
    val downloads: List<String>,
    val hashes: Map<String, String>
)

data class ModpackInfo(
    val name: String,
    val version: String,
    val mcVersion: String,
    val loader: ModLoader,
    val loaderVersion: String,
    val files: List<ModpackFile>,
    val overridesFolder: String
)

object ModpackParser {

    /** Parses a Modrinth .mrpack (modrinth.index.json) content. */
    fun parseMrpack(json: String): Result<ModpackInfo> = try {
        val root = JSONObject(json)
        val formatVersion = root.optInt("formatVersion", 1)
        val name = root.optString("name", "İsimsiz modpack")
        val version = root.optString("versionId", "?")
        val overrides = root.optString("overrides", "overrides")

        val deps = root.optJSONObject("dependencies") ?: JSONObject()
        val mcVersion = deps.optString("minecraft", "?")
        val loader = when {
            deps.has("fabric-loader") -> ModLoader.FABRIC
            deps.has("quilt-loader") -> ModLoader.QUILT
            deps.has("neoforge") -> ModLoader.NEOFORGE
            deps.has("forge") -> ModLoader.FORGE
            else -> ModLoader.VANILLA
        }
        val loaderVersion = when (loader) {
            ModLoader.FABRIC -> deps.optString("fabric-loader")
            ModLoader.QUILT -> deps.optString("quilt-loader")
            ModLoader.NEOFORGE -> deps.optString("neoforge")
            ModLoader.FORGE -> deps.optString("forge")
            ModLoader.VANILLA -> ""
        }

        val filesArr = root.optJSONArray("files")
        val files = mutableListOf<ModpackFile>()
        if (filesArr != null) {
            for (i in 0 until filesArr.length()) {
                val f = filesArr.getJSONObject(i)
                val path = f.optString("path")
                val downloadsArr = f.optJSONArray("downloads")
                val downloads = mutableListOf<String>()
                if (downloadsArr != null) {
                    for (j in 0 until downloadsArr.length()) downloads += downloadsArr.getString(j)
                }
                val hashesObj = f.optJSONObject("hashes")
                val hashes = mutableMapOf<String, String>()
                if (hashesObj != null) {
                    for (key in hashesObj.keys()) hashes[key] = hashesObj.optString(key)
                }
                files += ModpackFile(path, downloads, hashes)
            }
        }

        Result.success(
            ModpackInfo(
                name = name,
                version = version,
                mcVersion = mcVersion,
                loader = loader,
                loaderVersion = loaderVersion,
                files = files,
                overridesFolder = overrides
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun describe(info: ModpackInfo): String =
        "${info.name} ${info.version} • Minecraft ${info.mcVersion} • ${info.loader.display} " +
            "${info.loaderVersion} • ${info.files.size} dosya"
}
