package io.genai.ruby.sdk

enum class OsFamily { WINDOWS, MAC, LINUX }

/**
 * A downloadable portable Ruby build.
 *
 * The source is **Homebrew's portable-ruby**, the build Homebrew itself uses to bootstrap on a
 * machine with no usable Ruby. That matters: it is the only current Ruby distribution that is
 * genuinely relocatable. The obvious alternative — the `ruby/ruby-builder` tarballs that
 * `ruby/setup-ruby` consumes — cannot be used here. Those are built for the GitHub Actions runner
 * image: the binary hard-links `/Users/runner/hostedtoolcache/Ruby/<v>/<arch>/lib/libruby.dylib`
 * and its native extensions need Homebrew's `gmp`, `libyaml` and `openssl@3` at fixed
 * `/opt/homebrew/...` paths, so on a normal machine it fails to even load `json`.
 *
 * portable-ruby, by contrast, extracts anywhere, resolves `RbConfig::CONFIG["prefix"]` from its
 * own location at runtime, and has no external library dependencies at all.
 *
 * @param version  e.g. "3.4.6"
 * @param bottle   Homebrew's platform tag, e.g. "arm64_big_sur" — part of the asset name
 */
data class RubyRelease(
    val version: String,
    val os: OsFamily,
    val arch: String,        // x86_64 / arm64
    val bottle: String,
    val url: String,
) {
    /** Directory name under ~/.ruby-portable. */
    val dirName: String get() = "ruby-$version"

    val label: String get() = "Ruby $version  ·  ${os.name.lowercase()}/$arch"

    override fun toString(): String = label
}
