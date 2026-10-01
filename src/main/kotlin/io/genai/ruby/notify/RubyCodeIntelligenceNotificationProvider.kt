package io.genai.ruby.notify

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import io.genai.ruby.lang.RubyFileType
import io.genai.ruby.lang.RubyFiles
import io.genai.ruby.lsp.CodeIntelligenceSetup
import io.genai.ruby.settings.RubyConfigurable
import io.genai.ruby.settings.RubySettings
import io.genai.ruby.sdk.RubySdkType
import java.util.function.Function
import javax.swing.JComponent

/**
 * On a `.rs` file where our language layer is active (IDEs without native Ruby support), offer a
 * single click to turn on code intelligence (install ruby-analyzer + LSP4IJ). Disappears once both are
 * present. On RubyMine / Ultimate the native Ruby support handles this, so we stay quiet.
 */
class RubyCodeIntelligenceNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension?.lowercase() !in RubyFiles.EXTENSIONS) return null
        if (FileTypeManager.getInstance().getFileTypeByExtension("rs") != RubyFileType) return null

        val settings = RubySettings.getInstance()
        if (!settings.codeIntelligenceEnabled) return null
        if (settings.codeIntelligencePromptDismissed) return null
        val hasToolchain = settings.defaultSdk()?.homePath
            ?.let { RubySdkType.findRubyExecutable(it) } != null
        if (!hasToolchain) return null

        if (CodeIntelligenceSetup.isFullySetUp()) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                text("Turn on Ruby code intelligence — completion, go-to-definition and error highlighting.")
                createActionLabel("Enable code intelligence") {
                    CodeIntelligenceSetup.enable(project) {
                        EditorNotifications.getInstance(project).updateAllNotifications()
                    }
                }
                createActionLabel("Settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, RubyConfigurable::class.java)
                }
                // For people who only ever want to run .rs files. Hides the prompt for good;
                // Settings ▸ Ruby Portable still has the button to turn code intelligence on later.
                createActionLabel("Don't show again") {
                    RubySettings.getInstance().codeIntelligencePromptDismissed = true
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
            }
        }
    }
}
