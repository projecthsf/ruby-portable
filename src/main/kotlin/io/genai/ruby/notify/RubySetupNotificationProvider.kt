package io.genai.ruby.notify

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import io.genai.ruby.lang.RubyFiles
import io.genai.ruby.sdk.RubyInterpreterActions
import io.genai.ruby.settings.RubySettings
import io.genai.ruby.settings.RubyConfigurable
import java.util.function.Function
import javax.swing.JComponent

/**
 * On a .rs file, if no Ruby interpreter is configured yet, show a banner offering to download or add
 * one. Disappears automatically once a interpreter exists.
 */
class RubySetupNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension?.lowercase() !in RubyFiles.EXTENSIONS) return null
        if (RubySettings.getInstance().defaultSdk() != null) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                text("No Ruby interpreter configured — download a portable one to run this file.")
                createActionLabel("Download Ruby…") {
                    RubyInterpreterActions.downloadInteractively(project) { refresh(project) }
                }
                createActionLabel("Add from Disk…") {
                    RubyInterpreterActions.addFromDisk { refresh(project) }
                }
                createActionLabel("Settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, RubyConfigurable::class.java)
                }
            }
        }
    }

    private fun refresh(project: Project) {
        EditorNotifications.getInstance(project).updateAllNotifications()
    }
}
