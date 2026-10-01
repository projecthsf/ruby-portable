# Ruby Portable

Write & run Ruby in **IntelliJ IDEA Community** for free — no RubyMine license, no system-wide
Ruby install. IDEA Community doesn't bundle the official Ruby plugin (that's Ultimate / RubyMine
only), so this fills the gap.

## Features
- **Portable Ruby** — download a self-contained interpreter from inside the IDE; stored under
  `~/.ruby-portable` with its own `GEM_HOME`, so nothing touches a system Ruby, rbenv, rvm or your
  shell profile.
- **Run a `.rb` file** — ▶ gutter marker and right-click ▸ Run. With a `Gemfile` present the script
  runs under `bundle exec` from the Gemfile's directory; otherwise plain `ruby script.rb`.
- **Syntax highlighting** — lexer-based, covering the Ruby-specific cases a C-like lexer gets
  wrong: regex-vs-division, heredocs, `%w`/`%i`/`%q` literals, `?a` char literals, symbols,
  `@ivar`/`@@cvar`/`$global`, and `=begin`/`=end` blocks.
- **Code intelligence (optional)** — completion, go-to-definition, hover docs and diagnostics via
  **ruby-lsp** (Shopify's language server), bridged with the free **LSP4IJ** plugin. Installed as
  a gem into the portable interpreter. Fully offline.
- **Run bundler / gem / rake** — `bundle install|update|exec`, `gem install|list`, `rake` in a
  console.

On **RubyMine / IDEA Ultimate** the native Ruby support owns `.rb`, and this plugin steps aside
(`<incompatible-with>org.jetbrains.plugins.ruby</incompatible-with>`).

## Where the interpreter comes from — and what does NOT work

The download uses **Homebrew's portable-ruby**, the build Homebrew itself uses to bootstrap on a
machine with no usable Ruby. That is the point: it is relocatable. Verified by extracting a bottle
to an arbitrary path — with **no environment variables at all** it runs, `RbConfig::CONFIG["prefix"]`
resolves to the real location, it has **zero external dylib dependencies**, and
`gem install ruby-lsp` into an isolated `GEM_HOME` works.

**Do not switch to `ruby/ruby-builder`** (the tarballs `ruby/setup-ruby` consumes), however
convenient the version coverage looks. Those are built for the GitHub Actions runner image and are
not redistributable:

- the binary hard-links `/Users/runner/hostedtoolcache/Ruby/<v>/<arch>/lib/libruby.3.4.dylib`, so
  it won't even start elsewhere without `DYLD_FALLBACK_LIBRARY_PATH`
- the stdlib path is baked in too, so `require "json"` fails without `RUBYLIB`
- and fatally, its native extensions need Homebrew's `gmp`, `libyaml` and `openssl@3` at fixed
  `/opt/homebrew/...` paths — `psych` fails to load on any machine without exactly those formulae

All three were confirmed by hand before choosing portable-ruby.

## Windows

Not supported for in-IDE download yet. portable-ruby has no Windows build, and RubyInstaller ships
only `.7z` and `.exe` — the platform's `Decompressor` reads Zip and Tar only, and the IDE bundles
no 7z-capable library (checked: no commons-compress, no tukaani-xz). Windows users can point at an
existing Ruby with **Add from Disk…**, which works today.

Adding it later means either bundling commons-compress + xz (~1.5 MB) to unpack the `.7z`, or
driving the RubyInstaller `.exe` with Inno Setup's `/verysilent /dir=` flags.

## Architecture
A re-instantiation of the `rust-portable` skeleton for Ruby: portable interpreter downloader,
static `.rb` file-type claim, run configuration, LSP4IJ bridge (languageId **must** be `"ruby"`),
and a one-click "Enable code intelligence" onboarding banner.

Shebang scripts are the recurring trap: `gem`, `bundle`, `rake` and the `ruby-lsp` binstub are all
`#!/bin/sh` or `#!/usr/bin/env ruby` wrappers. Every one of them is passed as an **argument to the
portable `ruby`**, never executed directly — executing them directly resolves `ruby` from `PATH`
and can silently pick up a system interpreter, or fail where there is none.

Note the contrast with the Rust plugin: there, `rustup` pre-creates a `rust-analyzer` shim whether
or not the component is installed, so a file-existence check is a false positive. RubyGems does
**not** pre-create binstubs, so checking for `<GEM_HOME>/bin/ruby-lsp` is sound here.

## Build
```
JAVA_HOME=<a JBR 17> ./gradlew buildPlugin      # -> build/distributions/ruby-portable-*.zip
JAVA_HOME=<a JBR 17> ./gradlew runIde           # sandbox IDE for testing
JAVA_HOME=<a JBR 17> ./gradlew verifyPlugin     # JetBrains Plugin Verifier (publish gate)
```

Build with a JDK ≤ 21 — Kotlin 1.9.x's compiler crashes on JDK 24+. Don't run two Gradle builds
against this project at once; it corrupts the Kotlin incremental cache and fails `verifyPlugin`
for reasons unrelated to the plugin.

## Releasing
Publishing is decoupled from merging: `.github/workflows/publish.yml` triggers on `v*` tag pushes
and refuses to run when the tag disagrees with `version` in `build.gradle.kts`. Bump the version +
`<change-notes>`, merge, then `git tag vX.Y.Z && git push origin vX.Y.Z`.

**The first release must be uploaded by hand** at <https://plugins.jetbrains.com/plugin/add> — the
Marketplace API rejects a plugin it has never seen ("Cannot find plugin… upload manually at least
once to specify options like the license"). CI takes over from the second release on.
