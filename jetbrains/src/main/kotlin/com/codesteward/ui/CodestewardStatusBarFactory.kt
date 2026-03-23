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
        return if (health.reachable) "Codesteward" else "Codesteward (!)"
    }

    override fun getTooltipText(): String {
        val settings = CodestewardSettings.getInstance(project).state
        if (!settings.enabled) return "Codesteward Audit Proxy: Disabled"

        val health = HealthChecker.getInstance(project).getLastStatus()
        return if (health.reachable) {
            "Codesteward Audit Proxy: Connected${health.version?.let { " (v$it)" } ?: ""}"
        } else {
            "Codesteward Audit Proxy: Unreachable - ${health.error ?: "unknown"}"
        }
    }

    override fun getAlignment(): Float = Component.LEFT_ALIGNMENT

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        val settings = CodestewardSettings.getInstance(project)
        settings.state.enabled = !settings.state.enabled
        statusBar?.updateWidget(ID())
    }

    override fun dispose() {
        statusBar = null
    }
}
