package io.genai.ruby.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import io.genai.ruby.sdk.RubySdkType
import io.genai.ruby.settings.RubySettings
import java.io.File

class RubyRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String?,
) : RunConfigurationBase<RubyRunConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): RubyRunConfigurationOptions =
        super.getOptions() as RubyRunConfigurationOptions

    var scriptPath: String?
        get() = options.scriptPath
        set(value) { options.scriptPath = value }

    var sdkName: String?
        get() = options.sdkName
        set(value) { options.sdkName = value }

    var rubyPath: String?
        get() = options.rubyPath
        set(value) { options.rubyPath = value }

    var envs: MutableMap<String, String>
        get() = options.envs
        set(value) { options.envs = value }

    var passParentEnvs: Boolean
        get() = options.passParentEnvs
        set(value) { options.passParentEnvs = value }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        RubySettingsEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val script = scriptPath?.takeIf { it.isNotBlank() }
                    ?: throw ExecutionException("No Ruby file specified")
                val ruby = resolveRuby()
                    ?: throw ExecutionException(
                        "No Ruby interpreter configured — pick a Ruby SDK or set a ruby executable path",
                    )

                val scriptFile = File(script)
                val dir = scriptFile.parentFile
                val gemfileDir = dir?.let { findGemfileDir(it) }

                val cmd = GeneralCommandLine()
                cmd.exePath = ruby

                val home = resolveHome()
                val bundle = home?.let { RubySdkType.findBundleExecutable(it) }

                if (gemfileDir != null && bundle != null) {
                    // Under a Gemfile, run through Bundler so the script sees exactly the gems the
                    // project declares. Working dir is the Gemfile's directory — that is where
                    // Bundler looks, and where a terminal `bundle exec` would have run.
                    //
                    // `bundle` is itself a shebang script, so it goes as an ARGUMENT to the
                    // portable ruby rather than being executed directly; running it directly would
                    // resolve `ruby` from PATH and could pick up a system interpreter.
                    cmd.addParameter(bundle.absolutePath)
                    cmd.addParameter("exec")
                    cmd.addParameter("ruby")
                    cmd.addParameter(scriptFile.absolutePath)
                    cmd.setWorkDirectory(gemfileDir)
                } else {
                    cmd.addParameter(scriptFile.absolutePath)
                    dir?.let { cmd.setWorkDirectory(it) }
                }

                home?.let { cmd.withEnvironment(RubySdkType.environment(it)) }
                cmd.withEnvironment(options.envs)
                cmd.withParentEnvironmentType(
                    if (options.passParentEnvs) GeneralCommandLine.ParentEnvironmentType.CONSOLE
                    else GeneralCommandLine.ParentEnvironmentType.NONE,
                )

                val handler = OSProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }

    /** The nearest ancestor of [dir] (inclusive) containing a Gemfile, or null. */
    private fun findGemfileDir(dir: File): File? =
        generateSequence(dir) { it.parentFile }.firstOrNull { File(it, "Gemfile").isFile }

    /** An explicit ruby path wins; then the SDK pinned on this config; otherwise the default. */
    private fun resolveRuby(): String? {
        rubyPath?.takeIf { it.isNotBlank() }?.let { return it }

        val name = sdkName?.takeIf { it.isNotBlank() }
        val pinned = name?.let { ProjectJdkTable.getInstance().findJdk(it) }
        pinned?.homePath?.let { RubySdkType.findRubyExecutable(it) }?.let { return it.absolutePath }

        val default = RubySettings.getInstance().defaultSdk()
        return default?.homePath?.let { RubySdkType.findRubyExecutable(it)?.absolutePath }
    }

    /**
     * The SDK home backing this run, for GEM_HOME / GEM_PATH / PATH. Read from the registered SDK
     * rather than derived from the ruby path, since an "Add from Disk…" interpreter can have a
     * quite different layout from our own downloads. Null for an explicit ruby-path override.
     */
    private fun resolveHome(): String? {
        if (!rubyPath.isNullOrBlank()) return null

        val name = sdkName?.takeIf { it.isNotBlank() }
        val pinned = name?.let { ProjectJdkTable.getInstance().findJdk(it) }
        pinned?.homePath?.takeIf { RubySdkType.findRubyExecutable(it) != null }?.let { return it }

        return RubySettings.getInstance().defaultSdk()?.homePath
    }
}
