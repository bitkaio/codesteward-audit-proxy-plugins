package com.codesteward.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*

class CodestewardConfigurable(private val project: Project) :
    BoundConfigurable("Codesteward Audit Proxy") {

    private val settings get() = CodestewardSettings.getInstance(project)

    override fun createPanel() = panel {
        group("Proxy Connection") {
            row("Proxy URL:") {
                textField()
                    .bindText(settings.state::proxyUrl)
                    .columns(COLUMNS_LARGE)
                    .comment("URL of the Codesteward audit proxy")
            }
            row {
                checkBox("Enable proxy")
                    .bindSelected(settings.state::enabled)
            }
        }

        group("Identity") {
            row("User:") {
                textField()
                    .bindText(settings.state::user)
                    .columns(COLUMNS_LARGE)
                    .comment("Leave empty to auto-detect from git")
            }
            row("Project:") {
                textField()
                    .bindText(settings.state::project)
                    .columns(COLUMNS_LARGE)
                    .comment("Leave empty to auto-detect from workspace")
            }
            row("Branch:") {
                textField()
                    .bindText(settings.state::branch)
                    .columns(COLUMNS_LARGE)
                    .comment("Leave empty to auto-detect from git")
            }
            row("Team:") {
                textField()
                    .bindText(settings.state::team)
                    .columns(COLUMNS_LARGE)
            }
        }

        group("Agents") {
            row { checkBox("Claude Code CLI").bindSelected(settings.state::agentClaude) }
            row { checkBox("Codex CLI").bindSelected(settings.state::agentCodex) }
            row { checkBox("Gemini CLI").bindSelected(settings.state::agentGemini) }
            row { checkBox("Cline").bindSelected(settings.state::agentCline) }
            row { checkBox("Aider").bindSelected(settings.state::agentAider) }
            row { checkBox("Continue").bindSelected(settings.state::agentContinue) }
        }

        group("Health Check") {
            row {
                checkBox("Enable health checks")
                    .bindSelected(settings.state::healthCheckEnabled)
            }
            row("Interval (seconds):") {
                intTextField(5..300)
                    .bindIntText(settings.state::healthCheckIntervalSeconds)
            }
        }
    }
}
