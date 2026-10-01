package io.genai.ruby.settings

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import io.genai.ruby.lsp.CodeIntelligenceSetup
import io.genai.ruby.sdk.RubyInterpreterActions
import io.genai.ruby.sdk.RubySdkManager
import java.io.File
import javax.swing.DefaultListModel
import javax.swing.JComponent

/**
 * Settings ▸ Languages & Frameworks ▸ Ruby Portable — download, add-from-disk, and remove
 * portable Ruby interpreters, plus the code-intelligence toggle. Changes apply immediately.
 */
class RubyConfigurable : Configurable {

    private val model = DefaultListModel<Sdk>()
    private val list = JBList(model)

    override fun getDisplayName(): String = "Ruby Portable"

    override fun createComponent(): JComponent {
        list.cellRenderer = object : ColoredListCellRenderer<Sdk>() {
            override fun customizeCellRenderer(
                list: javax.swing.JList<out Sdk>,
                sdk: Sdk,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                append("${sdk.name}   —   ${sdk.homePath ?: "?"}")
            }
        }
        reload()

        return panel {
            row {
                comment(
                    "Ruby Portable interpreters. Downloads are stored under " +
                        "<code>~/.ruby-portable</code> and shared with Ruby run configurations.",
                )
            }
            row {
                cell(JBScrollPane(list)).align(Align.FILL)
            }.resizableRow()
            row {
                button("Download Ruby…") { downloadAction() }
                button("Add from Disk…") { addFromDiskAction() }
                button("Remove") { removeAction() }
                button("Clean Up") { cleanUpAction() }
                button("Open Folder") { openFolderAction() }
            }
            separator()
            row {
                checkBox("Code intelligence (completion, navigation, errors)")
                    .applyToComponent {
                        isSelected = RubySettings.getInstance().codeIntelligenceEnabled
                        addActionListener {
                            RubySettings.getInstance().codeIntelligenceEnabled = isSelected
                        }
                    }
                button(if (CodeIntelligenceSetup.isFullySetUp()) "Reinstall ruby-analyzer…" else "Enable Code Intelligence…") {
                    enableCodeIntelligenceAction()
                }
            }.rowComment(
                "Runs the official Ruby language server (ruby-lsp) on the selected interpreter — fully " +
                    "offline. Also installs the free <b>LSP4IJ</b> plugin (one click, may prompt a restart).",
            )
        }
    }

    private fun reload() {
        model.clear()
        RubySdkManager.listSdks().forEach { model.addElement(it) }
    }

    private fun downloadAction() = RubyInterpreterActions.downloadInteractively(null) { reload() }

    private fun addFromDiskAction() = RubyInterpreterActions.addFromDisk { reload() }

    private fun removeAction() {
        val sdk = list.selectedValue ?: return
        val home = sdk.homePath
        val managed = home != null &&
            File(home).absolutePath.startsWith(RubySdkManager.downloadRoot().toFile().absolutePath + File.separator)

        val deleteFiles: Boolean
        if (managed) {
            val answer = Messages.showYesNoCancelDialog(
                "Remove interpreter \"${sdk.name}\"?\n\nIt was downloaded to $home.",
                "Remove Ruby Interpreter",
                "Remove and Delete Files",
                "Remove Only",
                "Cancel",
                Messages.getQuestionIcon(),
            )
            when (answer) {
                Messages.YES -> deleteFiles = true
                Messages.NO -> deleteFiles = false
                else -> return
            }
        } else {
            val ok = Messages.showYesNoDialog(
                "Remove interpreter \"${sdk.name}\"?\n\n(Files on disk are left untouched.)",
                "Remove Ruby Interpreter",
                Messages.getQuestionIcon(),
            )
            if (ok != Messages.YES) return
            deleteFiles = false
        }

        RubySdkManager.remove(sdk, deleteFiles)
        reload()
    }

    private fun cleanUpAction() {
        val result = RubySdkManager.cleanUp()
        reload()
        Messages.showInfoMessage(
            "Removed ${result.removed} missing interpreter(s); registered ${result.added} orphaned install(s).",
            "Clean Up Ruby Interpreters",
        )
    }

    private fun enableCodeIntelligenceAction() {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project == null) {
            Messages.showInfoMessage(
                "Open a project first, then use the banner on a .rs file to enable code intelligence.",
                "Ruby Code Intelligence",
            )
            return
        }
        // Turning it on from here clears any earlier "Don't show again" on the editor banner,
        // so the two controls can't end up disagreeing.
        RubySettings.getInstance().codeIntelligencePromptDismissed = false
        CodeIntelligenceSetup.enable(project, force = CodeIntelligenceSetup.isFullySetUp()) {}
    }

    private fun openFolderAction() {
        val sdk = list.selectedValue
        val dir = sdk?.homePath?.let { File(it) } ?: RubySdkManager.downloadRoot().toFile()
        if (dir.exists()) RevealFileAction.openDirectory(dir)
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // Actions apply immediately; nothing to commit here.
    }
}
