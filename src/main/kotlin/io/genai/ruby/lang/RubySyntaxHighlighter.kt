package io.genai.ruby.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Colors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey as key
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class RubySyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = RubyLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            RubyTokenTypes.KEYWORD -> pack(KEYWORD)
            RubyTokenTypes.STRING -> pack(STRING)
            RubyTokenTypes.NUMBER -> pack(NUMBER)
            RubyTokenTypes.LINE_COMMENT -> pack(LINE_COMMENT)
            RubyTokenTypes.BLOCK_COMMENT -> pack(BLOCK_COMMENT)
            RubyTokenTypes.IDENTIFIER -> pack(IDENTIFIER)
            RubyTokenTypes.OPERATOR -> pack(OPERATOR)
            RubyTokenTypes.SYMBOL -> pack(SYMBOL)
            RubyTokenTypes.INSTANCE_VAR -> pack(INSTANCE_VAR)
            RubyTokenTypes.GLOBAL_VAR -> pack(GLOBAL_VAR)
            RubyTokenTypes.REGEX -> pack(REGEX)
            RubyTokenTypes.CONSTANT -> pack(CONSTANT)
            else -> EMPTY
        }

    companion object {
        val KEYWORD: TextAttributesKey = key("RUBY_KEYWORD", Colors.KEYWORD)
        val STRING: TextAttributesKey = key("RUBY_STRING", Colors.STRING)
        val NUMBER: TextAttributesKey = key("RUBY_NUMBER", Colors.NUMBER)
        val LINE_COMMENT: TextAttributesKey = key("RUBY_LINE_COMMENT", Colors.LINE_COMMENT)
        val BLOCK_COMMENT: TextAttributesKey = key("RUBY_BLOCK_COMMENT", Colors.BLOCK_COMMENT)
        val IDENTIFIER: TextAttributesKey = key("RUBY_IDENTIFIER", Colors.IDENTIFIER)
        val OPERATOR: TextAttributesKey = key("RUBY_OPERATOR", Colors.OPERATION_SIGN)
        val SYMBOL: TextAttributesKey = key("RUBY_SYMBOL", Colors.INSTANCE_FIELD)
        val INSTANCE_VAR: TextAttributesKey = key("RUBY_INSTANCE_VAR", Colors.INSTANCE_FIELD)
        val GLOBAL_VAR: TextAttributesKey = key("RUBY_GLOBAL_VAR", Colors.GLOBAL_VARIABLE)
        val REGEX: TextAttributesKey = key("RUBY_REGEX", Colors.STRING)
        val CONSTANT: TextAttributesKey = key("RUBY_CONSTANT", Colors.CONSTANT)
        private val EMPTY = emptyArray<TextAttributesKey>()
    }
}
