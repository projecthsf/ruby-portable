package io.genai.ruby.lang

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Binds our lightweight Ruby file type to `.rb` files — but ONLY in an IDE that has no official
 * Ruby support (i.e. IntelliJ IDEA Community, the whole reason this plugin exists).
 *
 * On RubyMine / IDEA Ultimate the official Ruby plugin owns `.rb`, so we stay dormant: our
 * `<fileType>` is declared with no extensions (see plugin.xml), so we never statically claim
 * `.rb` and there is nothing to clash. We only ever *add* the association, at runtime, when the
 * official plugin is absent — using the supported [FileTypeManager.associate] API.
 */
class RubyLanguageActivation : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (!activated.compareAndSet(false, true)) return
        if (rubyExtensionAlreadyOwned()) return

        val fileTypeManager = FileTypeManager.getInstance()
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                for (ext in RubyFiles.EXTENSIONS) {
                    fileTypeManager.associate(RubyFileType, ExtensionFileNameMatcher(ext))
                }
            }
        }
    }

    /**
     * Is `.rb` already claimed by some other file type? On RubyMine / IDEA Ultimate the official
     * Ruby plugin registers it at load — before this runs — so we defer to whoever owns it.
     * `UnknownFileType` means nobody owns it (Community); our own [RubyFileType] means we bound it
     * in a prior session (re-associating is harmless).
     */
    private fun rubyExtensionAlreadyOwned(): Boolean {
        val existing = FileTypeManager.getInstance().getFileTypeByExtension("rb")
        return existing != UnknownFileType.INSTANCE && existing != RubyFileType
    }

    companion object {
        private val activated = AtomicBoolean(false)
    }
}
