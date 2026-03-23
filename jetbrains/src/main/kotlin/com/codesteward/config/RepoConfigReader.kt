package com.codesteward.config

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.project.Project
import java.io.File

data class RepoConfig(
    val proxy: ProxyConfig? = null,
    val identity: IdentityConfig? = null,
    val headers: Map<String, String>? = null,
)

data class ProxyConfig(
    val url: String? = null,
    val enabled: Boolean? = null,
)

data class IdentityConfig(
    val team: String? = null,
    val project: String? = null,
    val user: String? = null,
)

object RepoConfigReader {
    private const val CONFIG_FILE_NAME = ".codesteward.json"
    private val gson = Gson()

    fun read(project: Project): RepoConfig? {
        val basePath = project.basePath ?: return null
        val configFile = File(basePath, CONFIG_FILE_NAME)
        if (!configFile.exists()) return null

        return try {
            gson.fromJson(configFile.readText(), RepoConfig::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
