package io.genai.ruby.lsp

import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import io.genai.ruby.settings.RubySettings

/**
 * Gates when the Ruby language server is active. LSP4IJ calls [isEnabled] before starting the
 * server for a file, so this enforces the "Code intelligence" toggle and the prerequisites
 * (an interpreter is configured, the ruby-lsp gem is installed). Returning false keeps the
 * server dormant.
 */
class RubyClientFeatures : LSPClientFeatures() {

    override fun isEnabled(file: VirtualFile): Boolean {
        val settings = RubySettings.getInstance()
        if (!settings.codeIntelligenceEnabled) return false
        return RubyLspManager.defaultHome() != null && RubyLspManager.isInstalled()
    }
}
