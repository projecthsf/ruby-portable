package io.genai.ruby.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.genai.ruby.lang.RubyFileType

/**
 * Puts a green ▶ Run marker at the top of every Ruby file, so a script launches with one click.
 *
 * Unlike Go's `func main()` or Rust's `fn main()`, Ruby has no entry-point declaration — any file
 * is runnable top to bottom. So the marker anchors on the file's first leaf, which is what the
 * Python plugin does for the same reason.
 */
class RubyRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.firstChild != null) return null // leaves only
        val file = element.containingFile ?: return null
        if (file.fileType != RubyFileType) return null
        if (PsiTreeUtil.getDeepestFirst(file) != element) return null

        val actions = ExecutorAction.getActions(0)
        if (actions.isEmpty()) return null
        return Info(AllIcons.RunConfigurations.TestState.Run, actions) { "Run Ruby script" }
    }
}
