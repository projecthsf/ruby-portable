package io.genai.ruby.run

import com.intellij.execution.configurations.RunConfigurationOptions

/** Persisted state for a Ruby run configuration. */
class RubyRunConfigurationOptions : RunConfigurationOptions() {
    private val scriptPathProp = string("").provideDelegate(this, "scriptPath")
    private val sdkNameProp = string("").provideDelegate(this, "sdkName")
    private val rubyPathProp = string("").provideDelegate(this, "rubyPath")

    var scriptPath: String?
        get() = scriptPathProp.getValue(this)
        set(value) = scriptPathProp.setValue(this, value)

    var sdkName: String?
        get() = sdkNameProp.getValue(this)
        set(value) = sdkNameProp.setValue(this, value)

    var rubyPath: String?
        get() = rubyPathProp.getValue(this)
        set(value) = rubyPathProp.setValue(this, value)

    /** User environment variables for the run (e.g. RUBY_LOG=debug). */
    var envs by map<String, String>()

    /** Whether to inherit the system/parent environment on top of [envs]. */
    var passParentEnvs by property(true)
}
