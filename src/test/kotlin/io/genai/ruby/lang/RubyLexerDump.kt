package io.genai.ruby.lang

import java.io.File

/**
 * Dumps the token stream [RubyLexer] produces for a file, so the Ruby-specific hard cases can be
 * checked without launching an IDE:
 *
 *     ./gradlew lexDump -PlexFile=examples/hello.rb
 *
 * Prints one line per non-blank token, then asserts the cases that silently corrupt the rest of a
 * file when the lexer gets them wrong — a regex read as division (or vice versa) and a heredoc
 * that doesn't terminate both swallow everything after them.
 */
object RubyLexerDump {

    @JvmStatic
    fun main(args: Array<String>) {
        val path = args.firstOrNull() ?: "examples/hello.rb"
        val text = File(path).readText()
        val tokens = lex(text)

        tokens.forEach { (type, raw) ->
            println("%-20s %s".format(type, raw.replace("\n", "\\n").take(72)))
        }

        println("\n--- checks ---")
        var failures = 0
        fun check(name: String, ok: Boolean) {
            if (!ok) failures++
            println("${if (ok) "PASS" else "FAIL"}  $name")
        }

        fun has(type: String, pred: (String) -> Boolean) =
            tokens.any { it.first == type && pred(it.second) }

        check("regex literal lexed as REGEX", has("RUBY_REGEX") { it.startsWith("/\\A") })
        check("division NOT lexed as a regex", !has("RUBY_REGEX") { it.contains("count") })
        check(
            "heredoc body is one STRING token",
            has("RUBY_STRING") { it.startsWith("<<~TEXT") && it.contains("terminator line") },
        )
        check("%w literal is a STRING", has("RUBY_STRING") { it.startsWith("%w[") })
        check("standalone ?R char literal is a STRING", has("RUBY_STRING") { it == "?R" })
        check("=begin block is a BLOCK_COMMENT", has("RUBY_BLOCK_COMMENT") { it.startsWith("=begin") })
        check("@name is an instance var", has("RUBY_INSTANCE_VAR") { it == "@name" })
        check("@@made is an instance var", has("RUBY_INSTANCE_VAR") { it == "@@made" })
        check("key: form lexed as a SYMBOL", has("RUBY_SYMBOL") { it == "name:" })
        check("constants lexed as CONSTANT", has("RUBY_CONSTANT") { it == "Greeter" })
        check("interpolated string stays one token", has("RUBY_STRING") { it.contains("#{i + 1}") })
        check("whole file consumed", tokens.isNotEmpty() && lexedLength(text) == text.length)

        println(if (failures == 0) "\nall checks passed" else "\n$failures check(s) FAILED")
    }

    private fun lex(text: String): List<Pair<String, String>> {
        val lexer = RubyLexer()
        lexer.start(text, 0, text.length, 0)
        val out = mutableListOf<Pair<String, String>>()
        while (lexer.tokenType != null) {
            val raw = text.substring(lexer.tokenStart, lexer.tokenEnd)
            if (raw.isNotBlank()) out += lexer.tokenType.toString() to raw
            lexer.advance()
        }
        return out
    }

    /** Where the lexer stopped — anything short of the file length means a token ran off a cliff. */
    private fun lexedLength(text: String): Int {
        val lexer = RubyLexer()
        lexer.start(text, 0, text.length, 0)
        var end = 0
        while (lexer.tokenType != null) {
            end = lexer.tokenEnd
            lexer.advance()
        }
        return end
    }
}
