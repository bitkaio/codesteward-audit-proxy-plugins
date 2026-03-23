package com.codesteward.ui

import com.codesteward.health.HealthChecker
import com.codesteward.health.HealthStatus
import com.codesteward.settings.CodestewardSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

class CodestewardStatusBarFactory : StatusBarWidgetFactory {
    override fun getId(): String = "CodestewardStatusBar"
    override fun getDisplayName(): String = "Codesteward Audit Proxy"
    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget =
        CodestewardStatusBarWidget(project)
}

class CodestewardStatusBarWidget(private val project: Project) :
    StatusBarWidget, StatusBarWidget.TextPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = "CodestewardStatusBar"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        val healthChecker = HealthChecker.getInstance(project)
        healthChecker.addListener { statusBar.updateWidget(ID()) }
    }

    override fun getText(): String {
        val settings = CodestewardSettings.getInstance(project).state
        if (!settings.enabled) return "Codesteward (off)"

        val health = HealthChecker.getInstance(project).getLastStatus()
        return when {
            health.reachable -> "Codesteward"
            health.error == "Not checked yet" -> "Codesteward (?)"
            else -> "Codesteward (!)"
        }
    }

    override fun getTooltipText(): String {
        val settings = CodestewardSettings.getInstance(project).state
        if (!settings.enabled) return "Codesteward Audit Proxy: Disabled — click to enable"

        val health = HealthChecker.getInstance(project).getLastStatus()
        return when {
            health.reachable ->
                "Codesteward Audit Proxy: Connected${health.version?.let { " (v$it)" } ?: ""}\n${settings.proxyUrl}"
            health.error == "Not checked yet" ->
                "Codesteward Audit Proxy: Checking...\n${settings.proxyUrl}"
            else ->
                "Codesteward Audit Proxy: Unreachable\n${settings.proxyUrl}\nError: ${health.error ?: "no response"}"
        }
    }

    override fun getAlignment(): Float = Component.LEFT_ALIGNMENT

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        val settings = CodestewardSettings.getInstance(project)
        val wasEnabled = settings.state.enabled
        settings.state.enabled = !wasEnabled

        if (!wasEnabled && settings.state.healthCheckEnabled) {
            // Just enabled — start health checking
            val healthChecker = HealthChecker.getInstance(project)
            healthChecker.start(settings.state.proxyUrl, settings.state.healthCheckIntervalSeconds)
        } else if (wasEnabled) {
            HealthChecker.getInstance(project).stop()
        }

        statusBar?.updateWidget(ID())
    }

    override fun dispose() {
        statusBar = null
    }
}
