package io.genai.ruby.sdk

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** Lets the user pick which portable Ruby interpreter to download. */
class RubyDownloadDialog(releases: List<RubyRelease>) : DialogWrapper(true) {
    private val combo = ComboBox(releases.toTypedArray())

    var selected: RubyRelease? = null
        private set

    init {
        title = "Download Ruby Interpreter"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Interpreter:") {
            cell(combo).align(AlignX.FILL)
        }
        row {
            comment("Downloaded to ~/.ruby-portable and registered as a Ruby SDK.")
        }
    }

    override fun doOKAction() {
        selected = combo.selectedItem as? RubyRelease
        super.doOKAction()
    }
}
