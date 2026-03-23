package com.codesteward.ui

import com.codesteward.config.RepoConfigReader
import com.codesteward.health.HealthChecker
import com.codesteward.identity.IdentityDetector
import com.codesteward.session.SessionManager
import com.codesteward.settings.CodestewardSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import javax.swing.JScrollPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class HeaderInspectorToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = HeaderInspectorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.component, "Headers", false)
        toolWindow.contentManager.addContent(content)
    }
}

class HeaderInspectorPanel(private val project: Project) {
    private val rootNode = DefaultMutableTreeNode("Codesteward Headers")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    val component = JScrollPane(tree)

    init {
        refresh()
        HealthChecker.getInstance(project).addListener { refresh() }
    }

    fun refresh() {
        rootNode.removeAllChildren()

        val settings = CodestewardSettings.getInstance(project).state

        if (!settings.enabled) {
            rootNode.add(DefaultMutableTreeNode("Proxy disabled"))
            treeModel.reload()
            return
        }

        val health = HealthChecker.getInstance(project).getLastStatus()
        val statusText = if (health.reachable) {
            "Connected${health.version?.let { " (v$it)" } ?: ""}"
        } else {
            "Unreachable - ${health.error ?: "unknown"}"
        }
        rootNode.add(DefaultMutableTreeNode("Status: $statusText"))

        val sessionId = SessionManager.getInstance(project).getSessionId()
        rootNode.add(DefaultMutableTreeNode("Session: ${sessionId.take(8)}..."))

        val repoConfig = RepoConfigReader.read(project)
        val identity = IdentityDetector.detect(project)

        val user = settings.user.ifEmpty { repoConfig?.identity?.user ?: identity.user }
        val projectName = settings.project.ifEmpty { repoConfig?.identity?.project ?: identity.project }
        val branch = settings.branch.ifEmpty { identity.branch }
        val team = settings.team.ifEmpty { repoConfig?.identity?.team ?: "" }

        // Injected Headers
        val headersNode = DefaultMutableTreeNode("Injected Headers")
        headersNode.add(DefaultMutableTreeNode("X-Audit-User: $user"))
        headersNode.add(DefaultMutableTreeNode("X-Audit-Project: $projectName"))
        headersNode.add(DefaultMutableTreeNode("X-Audit-Branch: $branch"))
        headersNode.add(DefaultMutableTreeNode("X-Audit-Session-ID: $sessionId"))
        if (team.isNotEmpty()) {
            headersNode.add(DefaultMutableTreeNode("X-Audit-Team: $team"))
        }
        rootNode.add(headersNode)

        // Agent Configuration
        val agentsNode = DefaultMutableTreeNode("Agent Configuration")
        if (settings.agentClaude) {
            val claude = DefaultMutableTreeNode("Claude Code CLI")
            claude.add(DefaultMutableTreeNode("ANTHROPIC_BASE_URL: ${settings.proxyUrl}"))
            agentsNode.add(claude)
        }
        if (settings.agentCodex) {
            val codex = DefaultMutableTreeNode("Codex CLI")
            codex.add(DefaultMutableTreeNode("OPENAI_BASE_URL: ${settings.proxyUrl}"))
            agentsNode.add(codex)
        }
        if (settings.agentGemini) {
            val gemini = DefaultMutableTreeNode("Gemini CLI")
            gemini.add(DefaultMutableTreeNode("GEMINI_API_BASE_URL: ${settings.proxyUrl}"))
            agentsNode.add(gemini)
        }
        rootNode.add(agentsNode)

        // Custom headers
        val allCustom = (repoConfig?.headers ?: emptyMap()) + settings.customHeaders
        if (allCustom.isNotEmpty()) {
            val customNode = DefaultMutableTreeNode("Custom Headers")
            allCustom.forEach { (k, v) -> customNode.add(DefaultMutableTreeNode("$k: $v")) }
            rootNode.add(customNode)
        }

        treeModel.reload()
        expandAll()
    }

    private fun expandAll() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }
}
