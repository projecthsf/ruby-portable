package io.genai.ruby.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Prompts for a bundler / gem / rake command: a dropdown of common ones, plus an argument field
 * that only appears for the commands that take one.
 */
class RunGemToolDialog(project: Project) : DialogWrapper(project) {

    private val commandCombo = ComboBox(COMMANDS)
    private val argField = JBTextField()
    private lateinit var argRow: Row

    init {
        title = "Run Bundler / Gem Tool"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row("Command:") { cell(commandCombo).align(AlignX.FILL) }
            row("Argument:") {
                cell(argField).align(AlignX.FILL)
                    .comment("e.g. rails, or a rake task name")
            }.also { argRow = it }
        }
        commandCombo.addActionListener { syncArgRow() }
        syncArgRow()
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = commandCombo

    private fun syncArgRow() = argRow.visible(needsArg())

    private fun needsArg(): Boolean =
        (commandCombo.selectedItem as? String) in setOf("gem install", "gem uninstall", "rake", "bundle exec")

    /**
     * The argv. The leading word selects the binary — `bundle`, `gem` or `rake` — and
     * [RunGemToolAction] resolves it against the portable interpreter.
     */
    val commandArgs: List<String>
        get() {
            val cmd = commandCombo.selectedItem as? String ?: return emptyList()
            val parts = cmd.split(" ")
            val arg = argField.text.trim()
            return if (needsArg() && arg.isNotEmpty()) parts + arg.split(Regex("\\s+")) else parts
        }

    companion object {
        private val COMMANDS = arrayOf(
            "bundle install", "bundle update", "bundle exec", "bundle list", "bundle outdated",
            "gem install", "gem uninstall", "gem list", "gem outdated",
            "rake", "rake -T",
        )
    }
}
