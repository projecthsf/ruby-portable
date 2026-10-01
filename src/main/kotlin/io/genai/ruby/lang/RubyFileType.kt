package io.genai.ruby.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * `.rb` file type. Referenced by plugin.xml via fieldName="INSTANCE" — a Kotlin `object`
 * already exposes a static `INSTANCE` field, so no extra declaration needed.
 */
object RubyFileType : LanguageFileType(RubyLanguage) {
    private val ICON: Icon = IconLoader.getIcon("/icons/ruby.svg", RubyFileType::class.java.classLoader)

    override fun getName(): String = "Ruby File"
    override fun getDescription(): String = "Ruby source file"
    override fun getDefaultExtension(): String = "rb"
    override fun getIcon(): Icon = ICON
}
