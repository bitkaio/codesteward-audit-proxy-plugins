package com.codesteward

import com.codesteward.config.RepoConfigReader
import com.codesteward.health.HealthChecker
import com.codesteward.settings.CodestewardSettings
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.codesteward.session.SessionManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CodestewardStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = CodestewardSettings.getInstance(project)
        LOG.info("Codesteward starting — enabled=${settings.state.enabled}, url=${settings.state.proxyUrl}")

        if (settings.state.enabled) {
            if (settings.state.healthCheckEnabled) {
                val healthChecker = HealthChecker.getInstance(project)
                healthChecker.start(
                    settings.state.proxyUrl,
                    settings.state.healthCheckIntervalSeconds,
                )
            } else {
                LOG.info("Health checks disabled, running a single connectivity check")
                // Even without periodic checks, do one check so the UI shows current state
                val healthChecker = HealthChecker.getInstance(project)
                val status = healthChecker.checkHealth(settings.state.proxyUrl)
                if (!status.reachable) {
                    LOG.warn("Initial proxy check failed: ${status.error}")
                }
            }
        } else if (!hasExplicitConfig(project)) {
            // Onboarding: show notification if proxy is not configured
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Codesteward")
                .createNotification(
                    "Codesteward Audit Proxy",
                    "Configure the audit proxy to capture AI agent activity with identity metadata.",
                    NotificationType.INFORMATION,
                )
                .addAction(NotificationAction.createSimpleExpiring("Configure") {
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, "Codesteward Audit Proxy")
                })
                .addAction(NotificationAction.createSimpleExpiring("Dismiss") {})
                .notify(project)
        }
    }

    private fun hasExplicitConfig(project: Project): Boolean {
        val settings = CodestewardSettings.getInstance(project).state
        val hasExplicitUrl = settings.proxyUrl != "http://localhost:8080"
        val hasRepoConfig = RepoConfigReader.read(project)?.proxy?.url != null
        return hasExplicitUrl || hasRepoConfig
    }

    companion object {
        private val LOG = Logger.getInstance(CodestewardStartupActivity::class.java)
    }
}

class CodestewardToggleAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = CodestewardSettings.getInstance(project)
        settings.state.enabled = !settings.state.enabled

        val healthChecker = HealthChecker.getInstance(project)
        if (settings.state.enabled && settings.state.healthCheckEnabled) {
            healthChecker.start(
                settings.state.proxyUrl,
                settings.state.healthCheckIntervalSeconds,
            )
        } else {
            healthChecker.stop()
        }
    }
}

class CodestewardOpenSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Codesteward Audit Proxy")
    }
}

class CodestewardShowHeadersAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Codesteward")
        toolWindow?.show()
    }
}

class CodestewardCopySessionIdAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sessionId = SessionManager.getInstance(project).getSessionId()
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(sessionId), null)
    }
}

class CodestewardRefreshIdentityAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Codesteward")
        toolWindow?.show()
    }
}

class CodestewardCheckHealthAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = CodestewardSettings.getInstance(project)
        val healthChecker = HealthChecker.getInstance(project)
        val proxyUrl = settings.state.proxyUrl
        val status = healthChecker.checkHealth(proxyUrl)

        val message = if (status.reachable) {
            "Proxy healthy${status.version?.let { " (v$it)" } ?: ""} at $proxyUrl"
        } else {
            "Cannot connect to $proxyUrl\n${status.error ?: "No response from proxy"}"
        }

        val type = if (status.reachable) NotificationType.INFORMATION else NotificationType.WARNING

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Codesteward")
            .createNotification("Codesteward", message, type)

        if (!status.reachable) {
            notification.addAction(NotificationAction.createSimpleExpiring("Open Settings") {
                ShowSettingsUtil.getInstance()
                    .showSettingsDialog(project, "Codesteward Audit Proxy")
            })
        }

        notification.notify(project)
    }
}
