package io.genai.ruby.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.projectRoots.Sdk
import io.genai.ruby.sdk.RubySdkManager
import io.genai.ruby.sdk.RubySdkType

/**
 * Remembers which Ruby interpreter is the "current" one. Application-level, since the interpreters
 * (SDKs) are application-level. Run configs with no explicit interpreter fall back to this.
 */
@Service(Service.Level.APP)
@State(name = "RubyPortable", storages = [Storage("ruby-portable.xml")])
class RubySettings : PersistentStateComponent<RubySettings.State> {

    class State {
        var defaultSdkName: String? = null
        // Code intelligence (ruby-analyzer completion, navigation, errors). Default ON — the headline
        // feature beyond "run a file". Gated via RubyClientFeatures.isEnabled.
        var codeIntelligenceEnabled: Boolean = true

        // The user dismissed the "turn on code intelligence" editor banner. Separate from
        // codeIntelligenceEnabled: this only hides the prompt, for people who just want to run
        // .rs files. Persisted, because editor notification panels are rebuilt on every file
        // open — a non-persistent dismiss would reappear immediately.
        var codeIntelligencePromptDismissed: Boolean = false
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    var defaultSdkName: String?
        get() = myState.defaultSdkName
        set(value) { myState.defaultSdkName = value }

    var codeIntelligenceEnabled: Boolean
        get() = myState.codeIntelligenceEnabled
        set(value) { myState.codeIntelligenceEnabled = value }

    var codeIntelligencePromptDismissed: Boolean
        get() = myState.codeIntelligencePromptDismissed
        set(value) { myState.codeIntelligencePromptDismissed = value }

    /** The selected interpreter, restricted to ones that still exist on disk, falling back to
     *  the first usable install. */
    fun defaultSdk(): Sdk? {
        val usable = RubySdkManager.listSdks().filter { RubySdkType.findRubyExecutable(it.homePath) != null }
        return usable.firstOrNull { it.name == myState.defaultSdkName } ?: usable.firstOrNull()
    }

    companion object {
        fun getInstance(): RubySettings = service()
    }
}
