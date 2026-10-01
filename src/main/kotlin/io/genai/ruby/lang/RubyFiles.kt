package io.genai.ruby.lang

/** Single source of truth for what counts as a Ruby file, by extension. */
object RubyFiles {
    /** Extensions we treat as Ruby. Used both to bind our file type (Community-only, see
     *  [RubyLanguageActivation]) and by the tooling, which keys off the extension directly.
     *
     *  Only `.rb` is claimed statically in plugin.xml — that is the extension the Marketplace
     *  recommendation indexes. `.rake`/`.gemspec` are added at runtime where nothing else owns
     *  them, since they are Ruby source in practice. */
    val EXTENSIONS: Set<String> = setOf("rb", "rake", "gemspec", "ru")
}
