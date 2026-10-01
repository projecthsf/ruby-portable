package io.genai.ruby.tools

import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import io.genai.ruby.lsp.RubyLspManager
import io.genai.ruby.sdk.RubySdkType
import java.io.File
import kotlin.io.path.Path

/**
 * Runs a bundler / gem / rake command with the portable Ruby in a Run console, using the project
 * directory as the working dir. No system Ruby needed; the environment is the isolated portable
 * setup (GEM_HOME under ~/.ruby-portable, interpreter on PATH).
 *
 * Each of `bundle`, `gem` and `rake` is a shebang script rather than a native binary, so they are
 * passed as an ARGUMENT to the portable ruby. Executing them directly would resolve `ruby` from
 * PATH and could silently use a system interpreter — or fail outright where there is none.
 */
class RunGemToolAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val base = project.basePath ?: run {
            Messages.showErrorDialog(project, "No project directory.", "Run Bundler / Gem Tool")
            return
        }

        val home = RubyLspManager.defaultHome()
        val ruby = RubyLspManager.defaultRuby()
        if (home == null || ruby == null) {
            Messages.showErrorDialog(
                project,
                "No Ruby interpreter configured. Set one up in Settings ▸ Ruby Portable.",
                "Run Bundler / Gem Tool",
            )
            return
        }

        val dialog = RunGemToolDialog(project)
        if (!dialog.showAndGet()) return
        val args = dialog.commandArgs
        if (args.isEmpty()) return

        val tool = resolveTool(home, args.first()) ?: run {
            Messages.showErrorDialog(
                project,
                "No ${args.first()} executable found in this interpreter.",
                "Run Bundler / Gem Tool",
            )
            return
        }

        val cmd = GeneralCommandLine()
            .withExePath(ruby.absolutePath)
            .withParameters(listOf(tool.absolutePath) + args.drop(1))
            .withWorkDirectory(base)
            .withEnvironment(RubySdkType.environment(home))
        val handler = OSProcessHandler(cmd)
        // bundler and gem write files (Gemfile.lock, installed gems) via an external process;
        // refresh the project dir when it finishes so changes show up without "Reload from Disk".
        handler.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                LocalFileSystem.getInstance().findFileByNioFile(Path(base))?.let {
                    VfsUtil.markDirtyAndRefresh(true, true, true, it)
                }
            }
        })
        RunContentExecutor(project, handler)
            .withTitle(args.joinToString(" "))
            .withActivateToolWindow(true)
            .run()
    }

    /** `bundle` and `gem` ship with the interpreter; `rake` may be a gem binstub instead. */
    private fun resolveTool(home: String, name: String): File? = when (name) {
        "bundle" -> RubySdkType.findBundleExecutable(home)
        "gem" -> RubySdkType.findGemExecutable(home)
        else -> File(File(RubySdkType.gemHome(home), "bin"), name).takeIf { it.isFile }
            ?: RubySdkType.findRubyExecutable(home)?.parentFile?.let { File(it, name) }?.takeIf { it.isFile }
    }
}
