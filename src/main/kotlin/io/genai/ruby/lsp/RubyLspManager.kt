package io.genai.ruby.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.util.SystemInfo
import io.genai.ruby.sdk.RubySdkType
import java.io.File

/**
 * Locates (and, on request, installs) **ruby-lsp** — Shopify's Ruby language server, which powers
 * code intelligence here.
 *
 * It installs as an ordinary gem into the interpreter's isolated GEM_HOME
 * (`gem install ruby-lsp`), so nothing lands in a system gem directory.
 *
 * Two things worth knowing about launching it:
 *
 *  - The binstub RubyGems writes is a **`#!/bin/sh` wrapper that re-execs `ruby`**, so running it
 *    directly would pick up whatever interpreter happens to be on PATH — possibly a system Ruby,
 *    possibly none. We always launch it as `<portable ruby> <GEM_HOME>/bin/ruby-lsp`.
 *  - Unlike rustup in the Rust plugin, RubyGems does **not** pre-create binstubs for gems that
 *    aren't installed, so "does this file exist" is a sound install check here. (In the Rust
 *    plugin it isn't: rustup creates a `rust-analyzer` shim at interpreter-install time whether or
 *    not the component is present.)
 */
object RubyLspManager {

    private const val GEM = "ruby-lsp"

    /** The ruby-lsp binstub inside [home]'s gem dir, whether or not it exists yet. */
    fun binFor(home: String): File {
        val bin = File(RubySdkType.gemHome(home), "bin")
        val plain = File(bin, GEM)
        // RubyGems writes a .bat wrapper alongside the binstub on Windows.
        return if (!plain.isFile && SystemInfo.isWindows) File(bin, "$GEM.bat") else plain
    }

    /** The ruby-lsp for the currently selected interpreter, if the gem is installed. */
    fun lspBin(): File? = defaultHome()?.let { binFor(it) }?.takeIf { it.isFile }

    fun isInstalled(): Boolean = lspBin() != null

    /** Environment for running ruby/gem/bundle/ruby-lsp against the portable interpreter. */
    fun environment(home: String): Map<String, String> = RubySdkType.environment(home)

    /**
     * Install the ruby-lsp gem into [home]'s GEM_HOME. Blocking — call off the EDT.
     * Throws on failure with the command output.
     */
    fun install(home: String) {
        val ruby = RubySdkType.findRubyExecutable(home)
            ?: throw RuntimeException(
                "No ruby binary found in this interpreter. Download one from Settings ▸ Ruby Portable.",
            )
        val gem = RubySdkType.findGemExecutable(home)
            ?: throw RuntimeException("No gem binary found next to $ruby.")

        // Invoke gem THROUGH the portable ruby: `gem` is itself a shebang script, and executing it
        // directly would resolve `ruby` from PATH rather than the interpreter we mean.
        val cmd = GeneralCommandLine(ruby.absolutePath, gem.absolutePath, "install", GEM, "--no-document")
            .withEnvironment(environment(home))
        val output = ExecUtil.execAndGetOutput(cmd)
        if (output.exitCode != 0 || !binFor(home).isFile) {
            throw RuntimeException(
                "gem install $GEM failed (exit ${output.exitCode}).\n" +
                    output.stderr.ifBlank { output.stdout }.take(1000),
            )
        }
    }

    /** Home directory of the current default interpreter, or null if none configured. */
    fun defaultHome(): String? =
        io.genai.ruby.settings.RubySettings.getInstance().defaultSdk()?.homePath
            ?.takeIf { RubySdkType.findRubyExecutable(it) != null }

    /** The current default interpreter's ruby binary, or null if none configured. */
    fun defaultRuby(): File? = defaultHome()?.let { RubySdkType.findRubyExecutable(it) }
}
