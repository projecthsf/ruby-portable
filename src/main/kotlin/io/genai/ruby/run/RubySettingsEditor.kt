package io.genai.ruby.run

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import io.genai.ruby.sdk.RubySdkType
import javax.swing.JComponent

class RubySettingsEditor(project: Project) : SettingsEditor<RubyRunConfiguration>() {

    private val sdkCombo = ComboBox<String>()
    private val rubyField = TextFieldWithBrowseButton()
    private val scriptField = TextFieldWithBrowseButton()
    private val envComponent = EnvironmentVariablesComponent()

    init {
        sdkCombo.addItem(NONE)
        ProjectJdkTable.getInstance().getSdksOfType(RubySdkType.getInstance()).forEach {
            sdkCombo.addItem(it.name)
        }
        scriptField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select Ruby File"),
                project,
            ),
        )
        rubyField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select ruby Executable"),
                project,
            ),
        )
    }

    override fun createEditor(): JComponent = panel {
        row("Ruby SDK:") {
            cell(sdkCombo).align(AlignX.FILL)
        }
        row("Or ruby executable:") {
            cell(rubyField).align(AlignX.FILL)
        }.rowComment("Overrides the SDK above when set.")
        row("Ruby file:") {
            cell(scriptField).align(AlignX.FILL)
        }
        row {
            cell(envComponent).align(AlignX.FILL)
        }
    }

    override fun resetEditorFrom(s: RubyRunConfiguration) {
        sdkCombo.selectedItem = s.sdkName?.takeIf { it.isNotBlank() } ?: NONE
        rubyField.text = s.rubyPath.orEmpty()
        scriptField.text = s.scriptPath.orEmpty()
        envComponent.envData = EnvironmentVariablesData.create(s.envs, s.passParentEnvs)
    }

    override fun applyEditorTo(s: RubyRunConfiguration) {
        val selected = sdkCombo.selectedItem as? String
        s.sdkName = if (selected == null || selected == NONE) "" else selected
        s.rubyPath = rubyField.text.trim()
        s.scriptPath = scriptField.text.trim()
        val data = envComponent.envData
        s.envs = HashMap(data.envs)
        s.passParentEnvs = data.isPassParentEnvs
    }

    companion object {
        private const val NONE = "<none>"
    }
}
