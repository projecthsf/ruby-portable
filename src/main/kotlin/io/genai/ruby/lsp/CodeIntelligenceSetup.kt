package io.genai.ruby.lsp

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.ThrowableComputable

/**
 * One-click setup for Ruby code intelligence. Two prerequisites the user would otherwise have to
 * piece together:
 *   1. ruby-lsp — added to the portable interpreter (`gem install ruby-lsp`).
 *   2. LSP4IJ — a JetBrains Marketplace plugin. We open the Plugins screen so the user installs
 *      it with one click, then restarts.
 *
 * Programmatic plugin install is intentionally avoided: the APIs for it are in the frontend-split
 * `app-client` module or marked `@ApiStatus.Internal` — both fail the JetBrains Plugin Verifier.
 * Opening the Plugins screen via its public configurable id is the supported, verifier-clean path.
 */
object CodeIntelligenceSetup {

    const val LSP4IJ_ID = "com.redhat.devtools.lsp4ij"
    private const val PLUGINS_CONFIGURABLE_ID = "preferences.pluginManager"

    fun isLsp4ijInstalled(): Boolean =
        PluginManager.isPluginInstalled(PluginId.getId(LSP4IJ_ID))

    /** Both prerequisites present — code intelligence can actually run. */
    fun isFullySetUp(): Boolean = isLsp4ijInstalled() && RubyLspManager.isInstalled()

    /**
     * Install ruby-lsp (if missing), then — if LSP4IJ isn't installed — open the Plugins
     * screen. [onChanged] runs after the ruby-lsp step so callers can refresh banners.
     * Must run on the EDT.
     */
    fun enable(project: Project, onChanged: () -> Unit) = enable(project, force = false, onChanged)

    /**
     * @param force re-run `gem install` even when ruby-lsp is already present.
     *   This is what the settings panel's "Reinstall" button needs; without it the call would
     *   early-return and the button would silently do nothing.
     */
    fun enable(project: Project, force: Boolean, onChanged: () -> Unit) {
        if (force || !RubyLspManager.isInstalled()) {
            val home = RubyLspManager.defaultHome()
            if (home == null) {
                Messages.showErrorDialog(
                    project,
                    "No Ruby interpreter configured yet. Download one in Settings ▸ Ruby Portable first.",
                    "Ruby Code Intelligence",
                )
                return
            }
            try {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    ThrowableComputable { RubyLspManager.install(home) },
                    if (force) "Reinstalling ruby-lsp…" else "Installing ruby-lsp…",
                    true,
                    project,
                )
                onChanged()
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "Failed to install ruby-lsp: ${e.message}",
                    "Ruby Code Intelligence",
                )
                return
            }
        }

        if (!isLsp4ijInstalled()) {
            Messages.showInfoMessage(
                project,
                "ruby-lsp is ready. One step left: in the Plugins window that opens, go to " +
                    "<b>Marketplace</b>, search <b>LSP4IJ</b>, click <b>Install</b>, then restart the IDE.",
                "Enable Ruby Code Intelligence",
            )
            ShowSettingsUtil.getInstance().showSettingsDialog(project, PLUGINS_CONFIGURABLE_ID)
        } else {
            onChanged()
        }
    }
}
