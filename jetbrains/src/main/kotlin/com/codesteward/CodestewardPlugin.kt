package com.codesteward

import com.codesteward.health.HealthChecker
import com.codesteward.settings.CodestewardSettings
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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

        if (settings.state.enabled && settings.state.healthCheckEnabled) {
            val healthChecker = HealthChecker.getInstance(project)
            healthChecker.start(
                settings.state.proxyUrl,
                settings.state.healthCheckIntervalSeconds,
            )
        }
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
        val status = healthChecker.checkHealth(settings.state.proxyUrl)

        val message = if (status.reachable) {
            "Proxy healthy${status.version?.let { " (v$it)" } ?: ""}"
        } else {
            "Proxy unreachable: ${status.error}"
        }

        val type = if (status.reachable) NotificationType.INFORMATION else NotificationType.WARNING

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Codesteward")
            .createNotification("Codesteward", message, type)
            .notify(project)
    }
}
