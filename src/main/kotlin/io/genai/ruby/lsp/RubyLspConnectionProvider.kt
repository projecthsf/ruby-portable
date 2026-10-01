package io.genai.ruby.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider

/**
 * Launches ruby-lsp over stdio against the portable interpreter.
 *
 * The command is `<portable ruby> <GEM_HOME>/bin/ruby-lsp`, not the binstub on its own: the
 * binstub is a `#!/bin/sh` wrapper that re-execs `ruby` from PATH, which could be a system Ruby
 * or nothing at all. [RubyClientFeatures.isEnabled] gates this, so if no interpreter or gem is
 * available we never get here.
 */
class RubyLspConnectionProvider(project: Project) : ProcessStreamConnectionProvider() {
    init {
        val home = RubyLspManager.defaultHome()
        val ruby = RubyLspManager.defaultRuby()
        val lsp = RubyLspManager.lspBin()
        if (home != null && ruby != null && lsp != null) {
            setCommands(listOf(ruby.absolutePath, lsp.absolutePath))
            project.basePath?.let { setWorkingDirectory(it) }
            setIncludeSystemEnvironmentVariables(true)
            setUserEnvironmentVariables(RubyLspManager.environment(home))
        }
    }
}
