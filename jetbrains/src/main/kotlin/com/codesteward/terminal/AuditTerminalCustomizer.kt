package com.codesteward.terminal

import com.codesteward.config.RepoConfigReader
import com.codesteward.identity.IdentityDetector
import com.codesteward.session.SessionManager
import com.codesteward.settings.CodestewardSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.LocalTerminalCustomizer

class AuditTerminalCustomizer : LocalTerminalCustomizer() {

    override fun customizeCommandAndEnvironment(
        project: Project,
        workingDirectory: String?,
        command: Array<out String>,
        envs: MutableMap<String, String>,
    ): Array<out String> {
        val settings = CodestewardSettings.getInstance(project).state
        if (!settings.enabled) return command

        val repoConfig = RepoConfigReader.read(project)
        val identity = IdentityDetector.detect(project)
        val sessionId = SessionManager.getInstance(project).getSessionId()

        val user = settings.user.ifEmpty { repoConfig?.identity?.user ?: identity.user }
        val projectName = settings.project.ifEmpty { repoConfig?.identity?.project ?: identity.project }
        val branch = settings.branch.ifEmpty { identity.branch }
        val team = settings.team.ifEmpty { repoConfig?.identity?.team ?: "" }
        val proxyUrl = settings.proxyUrl.ifEmpty { repoConfig?.proxy?.url ?: "http://localhost:8080" }

        // Build audit headers — always include all 5 X-Audit-* headers
        val headers = linkedMapOf(
            "X-Audit-User" to user,
            "X-Audit-Project" to projectName,
            "X-Audit-Branch" to branch,
            "X-Audit-Session-ID" to sessionId,
            "X-Audit-Team" to team,
        )
        repoConfig?.headers?.forEach { (k, v) -> headers[k] = v }
        settings.customHeaders.forEach { (k, v) -> headers[k] = v }

        // Newline-delimited for Anthropic SDK
        val newlineHeaders = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        // Comma-delimited for Gemini CLI
        val commaHeaders = headers.entries.joinToString(",") { "${it.key}: ${it.value}" }

        // Claude Code CLI
        if (settings.agentClaude) {
            envs["ANTHROPIC_BASE_URL"] = proxyUrl
            envs["ANTHROPIC_CUSTOM_HEADERS"] = newlineHeaders
        }

        // Codex CLI
        if (settings.agentCodex) {
            envs["OPENAI_BASE_URL"] = proxyUrl
        }

        // Gemini CLI
        if (settings.agentGemini) {
            envs["GEMINI_API_BASE_URL"] = proxyUrl
            envs["GEMINI_CLI_CUSTOM_HEADERS"] = commaHeaders
        }

        // Aider
        if (settings.agentAider) {
            envs["ANTHROPIC_BASE_URL"] = proxyUrl
            envs["OPENAI_BASE_URL"] = proxyUrl
            val jsonHeaders = headers.entries
                .joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }
            envs["AIDER_EXTRA_HEADERS"] = jsonHeaders
        }

        return command
    }
}
