package com.codesteward.ui

import com.codesteward.config.RepoConfigReader
import com.codesteward.health.HealthChecker
import com.codesteward.identity.IdentityDetector
import com.codesteward.session.SessionManager
import com.codesteward.settings.CodestewardSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class HeaderInspectorToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = HeaderInspectorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.component, "", false)
        toolWindow.contentManager.addContent(content)

        // Add toolbar actions
        val refreshAction = object : DumbAwareAction("Refresh", "Refresh identity and headers", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                panel.refresh()
            }
        }
        val healthAction = object : DumbAwareAction("Check Health", "Check proxy health", AllIcons.Actions.Execute) {
            override fun actionPerformed(e: AnActionEvent) {
                panel.refresh()
            }
        }
        toolWindow.setTitleActions(listOf(refreshAction, healthAction))
    }
}

data class InspectorNode(
    val label: String,
    val value: String = "",
    val icon: Icon? = null,
    val copyable: Boolean = false,
    val dimValue: Boolean = false,
)

class HeaderInspectorPanel(private val project: Project) {
    private val rootNode = DefaultMutableTreeNode()
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true

        // Custom cell renderer with icons and styled text
        cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean,
            ) {
                val node = value as? DefaultMutableTreeNode ?: return
                val data = node.userObject as? InspectorNode ?: return

                icon = data.icon
                append(data.label, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                if (data.value.isNotEmpty()) {
                    val attrs = if (data.dimValue) {
                        SimpleTextAttributes.GRAYED_ATTRIBUTES
                    } else {
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    }
                    append("  ${data.value}", attrs)
                }
            }
        }

        // Double-click to copy
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val path = getPathForLocation(e.x, e.y) ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                    val data = node.userObject as? InspectorNode ?: return
                    if (data.copyable && data.value.isNotEmpty()) {
                        CopyPasteManager.getInstance().setContents(StringSelection(data.value))
                    }
                }
            }
        })
    }
    val component = JPanel(BorderLayout()).apply {
        add(tree, BorderLayout.CENTER)
    }

    init {
        refresh()
        HealthChecker.getInstance(project).addListener { refresh() }
    }

    fun refresh() {
        rootNode.removeAllChildren()

        val settings = CodestewardSettings.getInstance(project).state

        if (!settings.enabled) {
            rootNode.add(DefaultMutableTreeNode(InspectorNode(
                "Proxy disabled",
                "Enable in Settings > Tools > Codesteward",
                AllIcons.RunConfigurations.TestIgnored,
            )))
            treeModel.reload()
            return
        }

        // Status
        val health = HealthChecker.getInstance(project).getLastStatus()
        val statusNode = if (health.reachable) {
            val version = health.version?.let { "v$it" } ?: ""
            DefaultMutableTreeNode(InspectorNode(
                "Connected",
                listOf(settings.proxyUrl, version).filter { it.isNotEmpty() }.joinToString(" \u2014 "),
                AllIcons.RunConfigurations.TestPassed,
            ))
        } else {
            val errorNode = DefaultMutableTreeNode(InspectorNode(
                "Unreachable",
                settings.proxyUrl,
                AllIcons.RunConfigurations.TestFailed,
            ))
            errorNode.add(DefaultMutableTreeNode(InspectorNode(
                "Error",
                health.error ?: "No response from proxy",
                AllIcons.General.Error,
                copyable = true,
            )))
            errorNode
        }
        rootNode.add(statusNode)

        // Identity section
        val repoConfig = RepoConfigReader.read(project)
        val identity = IdentityDetector.detect(project)
        val sessionId = SessionManager.getInstance(project).getSessionId()

        val user = settings.user.ifEmpty { repoConfig?.identity?.user ?: identity.user }
        val projectName = settings.project.ifEmpty { repoConfig?.identity?.project ?: identity.project }
        val branch = settings.branch.ifEmpty { identity.branch }
        val team = settings.team.ifEmpty { repoConfig?.identity?.team ?: "" }

        val identityNode = DefaultMutableTreeNode(InspectorNode(
            "Identity",
            "${listOf(user, projectName, branch, team).count { it.isNotEmpty() }} headers active",
            AllIcons.Nodes.SecurityRole,
        ))
        identityNode.add(DefaultMutableTreeNode(InspectorNode(
            "User",
            user.ifEmpty { "(empty)" },
            AllIcons.General.User,
            copyable = true,
        )))
        identityNode.add(DefaultMutableTreeNode(InspectorNode(
            "Project",
            projectName.ifEmpty { "(empty)" },
            AllIcons.Nodes.Module,
            copyable = true,
        )))
        identityNode.add(DefaultMutableTreeNode(InspectorNode(
            "Branch",
            branch,
            AllIcons.Vcs.Branch,
            copyable = true,
        )))
        identityNode.add(DefaultMutableTreeNode(InspectorNode(
            "Session",
            "${sessionId.take(8)}...",
            AllIcons.Nodes.PpLib,
            copyable = true,
        )))
        identityNode.add(DefaultMutableTreeNode(InspectorNode(
            "Team",
            team.ifEmpty { "(empty)" },
            AllIcons.Nodes.HomeFolder,
            copyable = true,
        )))
        rootNode.add(identityNode)

        // Agent Configuration
        val agentsNode = DefaultMutableTreeNode(InspectorNode(
            "Agents",
            "",
            AllIcons.Nodes.Toolbox,
        ))
        if (settings.agentClaude) {
            val claude = DefaultMutableTreeNode(InspectorNode("Claude Code CLI", "", AllIcons.Nodes.Console))
            claude.add(DefaultMutableTreeNode(InspectorNode("ANTHROPIC_BASE_URL", settings.proxyUrl, AllIcons.General.Web, copyable = true, dimValue = true)))
            agentsNode.add(claude)
        }
        if (settings.agentCodex) {
            val codex = DefaultMutableTreeNode(InspectorNode("Codex CLI", "", AllIcons.Nodes.Console))
            codex.add(DefaultMutableTreeNode(InspectorNode("OPENAI_BASE_URL", settings.proxyUrl, AllIcons.General.Web, copyable = true, dimValue = true)))
            agentsNode.add(codex)
        }
        if (settings.agentGemini) {
            val gemini = DefaultMutableTreeNode(InspectorNode("Gemini CLI", "", AllIcons.Nodes.Console))
            gemini.add(DefaultMutableTreeNode(InspectorNode("GEMINI_API_BASE_URL", settings.proxyUrl, AllIcons.General.Web, copyable = true, dimValue = true)))
            agentsNode.add(gemini)
        }
        if (settings.agentAider) {
            val aider = DefaultMutableTreeNode(InspectorNode("Aider", "", AllIcons.Nodes.Console))
            aider.add(DefaultMutableTreeNode(InspectorNode("ANTHROPIC_BASE_URL", settings.proxyUrl, AllIcons.General.Web, copyable = true, dimValue = true)))
            aider.add(DefaultMutableTreeNode(InspectorNode("OPENAI_BASE_URL", settings.proxyUrl, AllIcons.General.Web, copyable = true, dimValue = true)))
            agentsNode.add(aider)
        }
        rootNode.add(agentsNode)

        // Custom headers
        val allCustom = (repoConfig?.headers ?: emptyMap()) + settings.customHeaders
        if (allCustom.isNotEmpty()) {
            val customNode = DefaultMutableTreeNode(InspectorNode(
                "Custom Headers",
                "${allCustom.size} headers",
                AllIcons.General.Settings,
            ))
            allCustom.forEach { (k, v) ->
                customNode.add(DefaultMutableTreeNode(InspectorNode(k, v, AllIcons.Nodes.Tag, copyable = true)))
            }
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
