package io.genai.ruby.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

/**
 * Registers the Ruby language server (ruby-lsp) with LSP4IJ. Declared via the LSP4IJ `server` +
 * `languageMapping` extension points in META-INF/lsp.xml (an optional module that loads only
 * when LSP4IJ is installed).
 */
class RubyLanguageServerFactory : LanguageServerFactory {

    override fun createConnectionProvider(project: Project): StreamConnectionProvider =
        RubyLspConnectionProvider(project)

    override fun createClientFeatures(): LSPClientFeatures = RubyClientFeatures()
}
