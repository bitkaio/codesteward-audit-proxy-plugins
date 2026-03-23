package com.codesteward.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "CodestewardSettings",
    storages = [Storage("codesteward.xml")]
)
class CodestewardSettings : PersistentStateComponent<CodestewardSettings.State> {
    data class State(
        var proxyUrl: String = "http://localhost:8080",
        var enabled: Boolean = false,
        var user: String = "",
        var project: String = "",
        var branch: String = "",
        var team: String = "",
        var customHeaders: MutableMap<String, String> = mutableMapOf(),
        var agentClaude: Boolean = true,
        var agentCodex: Boolean = true,
        var agentGemini: Boolean = true,
        var agentCline: Boolean = true,
        var agentAider: Boolean = true,
        var agentContinue: Boolean = true,
        var healthCheckEnabled: Boolean = true,
        var healthCheckIntervalSeconds: Int = 30,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): CodestewardSettings =
            project.getService(CodestewardSettings::class.java)
    }
}
