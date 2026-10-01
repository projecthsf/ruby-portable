package io.genai.ruby.lang

import com.intellij.lang.Language

/**
 * The Ruby language for this plugin.
 *
 * The ID is deliberately **"RubyPortable"**, NOT "ruby": IntelliJ requires language IDs to be
 * globally unique, and the official JetBrains Ruby plugin (org.jetbrains.plugins.ruby, on
 * RubyMine / IDEA Ultimate) already registers ID "ruby". A distinct ID lets us coexist quietly;
 * the display name is still "Ruby". The ID must match the `language=` attributes in plugin.xml.
 */
object RubyLanguage : Language("RubyPortable") {
    override fun getDisplayName(): String = "Ruby"
}
