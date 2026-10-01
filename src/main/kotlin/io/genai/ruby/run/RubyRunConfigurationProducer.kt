package io.genai.ruby.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import io.genai.ruby.lang.RubyFiles

/**
 * Makes a `.rs` file runnable directly: right-click ▸ Run, and a gutter ▶ marker.
 * Auto-fills the file path; the run follows the current default interpreter unless pinned.
 *
 * Ruby has no entry-point convention to detect, so any .rb file is a valid run target.
 */
class RubyRunConfigurationProducer : LazyRunConfigurationProducer<RubyRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(RubyRunConfigurationType::class.java)
            .configurationFactories[0]

    override fun setupConfigurationFromContext(
        configuration: RubyRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val file = rubyFile(context) ?: return false
        configuration.scriptPath = file.path
        configuration.name = file.name
        return true
    }

    override fun isConfigurationFromContext(
        configuration: RubyRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val file = rubyFile(context) ?: return false
        val script = configuration.scriptPath ?: return false
        return FileUtil.pathsEqual(script, file.path)
    }

    private fun rubyFile(context: ConfigurationContext): VirtualFile? {
        val vf = CommonDataKeys.VIRTUAL_FILE.getData(context.dataContext)
            ?: context.psiLocation?.containingFile?.virtualFile
        if (vf == null || vf.isDirectory) return null
        return if (vf.extension?.lowercase() in RubyFiles.EXTENSIONS) vf else null
    }
}
