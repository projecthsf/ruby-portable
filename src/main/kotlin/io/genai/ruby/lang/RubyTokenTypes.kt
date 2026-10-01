package io.genai.ruby.lang

import com.intellij.psi.tree.IElementType

object RubyTokenTypes {
    @JvmField val KEYWORD = IElementType("RUBY_KEYWORD", RubyLanguage)
    @JvmField val IDENTIFIER = IElementType("RUBY_IDENTIFIER", RubyLanguage)
    @JvmField val STRING = IElementType("RUBY_STRING", RubyLanguage)
    @JvmField val NUMBER = IElementType("RUBY_NUMBER", RubyLanguage)
    @JvmField val LINE_COMMENT = IElementType("RUBY_LINE_COMMENT", RubyLanguage)
    @JvmField val BLOCK_COMMENT = IElementType("RUBY_BLOCK_COMMENT", RubyLanguage)
    @JvmField val OPERATOR = IElementType("RUBY_OPERATOR", RubyLanguage)

    /** `:name`, `:"quoted"` — and the `name:` form of a hash key. */
    @JvmField val SYMBOL = IElementType("RUBY_SYMBOL", RubyLanguage)

    /** `@ivar` and `@@cvar`. */
    @JvmField val INSTANCE_VAR = IElementType("RUBY_INSTANCE_VAR", RubyLanguage)

    /** `$global` and the special `$1`, `$!`, `$LOAD_PATH`. */
    @JvmField val GLOBAL_VAR = IElementType("RUBY_GLOBAL_VAR", RubyLanguage)

    /** `/pattern/flags` — only where a regex can legally start. */
    @JvmField val REGEX = IElementType("RUBY_REGEX", RubyLanguage)

    /** A constant or class name — an identifier that starts uppercase. */
    @JvmField val CONSTANT = IElementType("RUBY_CONSTANT", RubyLanguage)

    /** Ruby's reserved words. */
    val KEYWORDS: Set<String> = setOf(
        "__ENCODING__", "__LINE__", "__FILE__", "BEGIN", "END",
        "alias", "and", "begin", "break", "case", "class", "def", "defined?", "do", "else",
        "elsif", "end", "ensure", "false", "for", "if", "in", "module", "next", "nil", "not",
        "or", "redo", "rescue", "retry", "return", "self", "super", "then", "true", "undef",
        "unless", "until", "when", "while", "yield",
    )

    /**
     * Not reserved words, but universally read as language-level in Ruby and highlighted as such
     * by most editors.
     */
    val PSEUDO_KEYWORDS: Set<String> = setOf(
        "require", "require_relative", "load", "include", "extend", "prepend",
        "attr_accessor", "attr_reader", "attr_writer", "private", "protected", "public",
        "module_function", "raise", "lambda", "proc", "new",
    )
}
