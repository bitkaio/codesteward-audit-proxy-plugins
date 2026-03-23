package com.codesteward.session

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.util.UUID

@Service(Service.Level.PROJECT)
@State(
    name = "CodestewardSession",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class SessionManager : PersistentStateComponent<SessionManager.State> {
    data class State(
        var sessionId: String = UUID.randomUUID().toString(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getSessionId(): String = state.sessionId

    fun regenerate(): String {
        state.sessionId = UUID.randomUUID().toString()
        return state.sessionId
    }

    companion object {
        fun getInstance(project: Project): SessionManager =
            project.getService(SessionManager::class.java)
    }
}
