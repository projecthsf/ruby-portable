#!/usr/bin/env python3
"""
Generates the JetBrains Marketplace screenshots for Ruby Portable.

Renders each screen as a 1200x760 HTML page, then screenshots it with headless
Chrome at device-scale 2 to produce the 2400x1520 PNGs the other Portable
plugins use (see python-portable/media/marketplace).

    python3 build.py

Every string shown here is copied from the plugin's own source — banner text,
button labels, dialog titles, the toolchain list. If you change UI copy, change
it here too or the listing drifts from the product.
"""

import html
import pathlib
import shutil
import subprocess
import sys

SRC = pathlib.Path(__file__).resolve().parent
OUT = SRC.parent / "marketplace"
WORK = SRC / ".render"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# --- the sample file shown in the editor -------------------------------------
# Valid Rust, chosen to exercise exactly the constructs the lexer handles
# specially: doc comments (//! and ///), attributes, lifetimes vs char
# literals, raw strings, macros, and suffixed hex literals.
CODE = [
    ('<span class="c"># Formats user greetings.</span>', "run"),
    ("", ""),
    ('<span class="k">class</span> <span class="co">Greeter</span>', ""),
    ('  <span class="co">GREETINGS</span> = <span class="s">%w[Hello Xin\ chào Hej]</span>.<span class="t">freeze</span>', ""),
    ("", ""),
    ('  <span class="k">def</span> <span class="f">initialize</span>(<span class="sym">name:</span>, <span class="sym">times:</span> <span class="n">3</span>)', ""),
    ('    <span class="sym">@name</span>  = name', ""),
    ('    <span class="sym">@times</span> = times', ""),
    ("  <span class=\"k\">end</span>", ""),
    ("", ""),
    ('  <span class="c"># Symbols, interpolation, and a predicate method.</span>', ""),
    ('  <span class="k">def</span> <span class="f">shouty?</span> = <span class="sym">@name</span> == <span class="sym">@name</span>.<span class="t">upcase</span>', ""),
    ("", ""),
    ('  <span class="k">def</span> <span class="f">greet</span>', ""),
    ('    <span class="sym">@times</span>.<span class="t">times</span>.<span class="t">map</span> { |i| <span class="s">"#{i + 1}. #{GREETINGS.sample}, #{@name}!"</span> }', ""),
    ("  <span class=\"k\">end</span>", ""),
    ("<span class=\"k\">end</span>", ""),
    ("", ""),
    ('<span class="c"># `/` opens a REGEX here — but in `total / count` below it is division.</span>', ""),
    ('<span class="co">SLUG</span> = <span class="s">/\A[a-z0-9\-]+\z/i</span>', ""),
    ("", ""),
    ('report = <span class="s">&lt;&lt;~TEXT</span>', ""),
    ('<span class="s">  A squiggly heredoc keeps its own indentation</span>', ""),
    ('<span class="s">  and runs until the terminator line.</span>', ""),
    ('<span class="s">TEXT</span>', ""),
    ("", ""),
    ('total, count = <span class="n">10</span>, <span class="n">4</span>', ""),
    ('average = total / count           <span class="c"># division, not a regex</span>', ""),
    ('initial = <span class="s">?R</span>                      <span class="c"># one-char string</span>', ""),
    ("", ""),
    ('<span class="t">puts</span> <span class="co">Greeter</span>.<span class="k">new</span>(<span class="sym">name:</span> <span class="s">"Ruby"</span>, <span class="sym">times:</span> <span class="n">2</span>).<span class="t">greet</span>', ""),
    ('<span class="t">puts</span> <span class="s">"slug ok: #{SLUG.match?(\'ruby-portable\')}"</span>', ""),
]

TREE = """
      <div class="tree">
        <div class="hdr">PROJECT</div>
        <div class="row"><span class="ic ic-folder">▾</span>greeter</div>
        <div class="row nest"><span class="ic ic-folder">▸</span>lib</div>
        <div class="row sel" style="padding-left:54px"><span class="ic ic-rs">R</span>greeter.rb</div>
        <div class="row nest"><span class="ic ic-file">☰</span>Gemfile</div>
      </div>
"""


def editor(rows=CODE, banner="", console="", extra=""):
    gutter, code = [], []
    for i, (line, mark) in enumerate(rows, 1):
        gutter.append(f'<span class="run">▶</span>' if mark == "run" else str(i))
        code.append(line if line else "&nbsp;")
    return f"""
      <div class="main">
        <div class="tabs"><div class="tab"><span class="ic ic-rs">R</span>greeter.rb</div></div>
        {banner}
        <div class="editor">
          <div class="gutter">{"<br>".join(gutter)}</div>
          <div class="code">{"<br>".join(code)}</div>
          {extra}
        </div>
        {console}
      </div>
"""


def page(title, inner, footer=None):
    """footer=None gives the editor status bar; pass HTML for a dialog button row."""
    bottom = footer if footer is not None else (
        '<div class="status"><span>Ruby 3.4.6</span><span>UTF-8</span></div>'
    )
    return f"""<!doctype html>
<html><head><meta charset="utf-8"><link rel="stylesheet" href="shell.css"></head>
<body><div class="window">
  <div class="titlebar">
    <div class="lights"><i class="light red"></i><i class="light amber"></i><i class="light green"></i></div>
    <div class="title">{html.escape(title)}</div>
  </div>
  <div class="body">{inner}</div>
  {bottom}
</div></body></html>
"""


SETTINGS_FOOTER = """
  <div class="sfooter">
    <div class="btn">Cancel</div><div class="btn">Apply</div><div class="btn pri">OK</div>
  </div>
"""


BANNER_SETUP = """
        <div class="banner">
          <span class="info">i</span>
          <span>No Ruby interpreter configured — download a portable one to run this file.</span>
          <span class="spacer"></span>
          <a>Download Ruby…</a><a>Add from Disk…</a><a>Settings…</a>
        </div>
"""

BANNER_CI = """
        <div class="banner">
          <span class="info">i</span>
          <span>Turn on Ruby code intelligence — completion, go-to-definition and error highlighting.</span>
          <span class="spacer"></span>
          <a>Enable code intelligence</a><a>Settings…</a><a>Don't show again</a>
        </div>
"""

CONSOLE = """
        <div class="console">
          <div class="ch"><span style="color:#3f9c35">▶</span><span>Run:</span><span style="color:#d6d6d6">greeter.rb</span></div>
          <div class="cb">1. Xin chào, Ruby!
2. Hello, Ruby!
slug ok: true
average: 2, hex: 255, big: 1000000
initial: R, greeters made: 1
A squiggly heredoc keeps its own indentation
and runs until the terminator line.

<span class="dim">Process finished with exit code 0</span></div>
        </div>
"""

DIALOG_DOWNLOAD = """
          <div class="scrim"></div>
          <div class="dialog" style="width:470px">
            <div class="dh">Download Ruby Interpreter</div>
            <div class="dc">
              <div class="row2">
                <label>Interpreter:</label>
                <div class="field combo"><span>Ruby 3.4.6  ·  mac/arm64</span><span class="caret">▾</span></div>
              </div>
              <div class="hint" style="margin-left:98px">Downloaded to ~/.ruby-portable and registered as a Ruby SDK.</div>
            </div>
            <div class="df"><div class="btn">Cancel</div><div class="btn pri">OK</div></div>
          </div>
"""

DIALOG_CARGO = """
          <div class="scrim"></div>
          <div class="dialog" style="width:430px">
            <div class="dh">Run Bundler / Gem Tool</div>
            <div class="dc">
              <div class="row2">
                <label>Command:</label>
                <div class="field combo" style="flex:0 0 190px"><span>bundle install</span><span class="caret">▾</span></div>
              </div>
            </div>
            <div class="df"><div class="btn">Cancel</div><div class="btn pri">OK</div></div>
          </div>
"""

SETTINGS_TREE = """
      <div class="stree">
        <div class="trow"><span class="tw">▸</span>Appearance &amp; Behavior</div>
        <div class="trow"><span class="tw"></span>Keymap</div>
        <div class="trow"><span class="tw">▸</span>Editor</div>
        <div class="trow"><span class="tw"></span>Plugins</div>
        <div class="trow"><span class="tw">▾</span>Languages &amp; Frameworks</div>
        <div class="trow sel" style="padding-left:52px"><span class="ic ic-rs" style="margin-right:8px">R</span>Ruby Portable</div>
        <div class="trow" style="padding-left:52px">Markdown</div>
        <div class="trow"><span class="tw">▸</span>Build, Execution, Deployment</div>
      </div>
"""

SETTINGS = """
      <div class="settings">
        <div class="sh">Ruby Portable</div>
        <div class="sb">
          <div class="sdesc">Ruby Portable interpreters. Downloads are stored under <span style="color:#cc7832">~/.ruby-portable</span> and shared with Ruby run configurations.</div>
          <div class="list">
            <div class="li sel">Ruby 3.4.6   —   ~/.ruby-portable/ruby-3.4.6</div>
            <div class="li">Ruby 3.3.8   —   ~/.ruby-portable/ruby-3.3.8</div>
          </div>
          <div class="btnrow">
            <div class="btn">Download Ruby…</div><div class="btn">Add from Disk…</div>
            <div class="btn">Remove</div><div class="btn">Clean Up</div><div class="btn">Open Folder</div>
          </div>
          <div class="sep"></div>
          <div class="check"><span class="box">✓</span>Code intelligence (completion, navigation, errors)
            <span style="margin-left:14px"><span class="btn">Reinstall ruby-lsp…</span></span>
          </div>
          <div class="sdesc" style="margin-top:6px">Runs the official Ruby language server (ruby-lsp) on the selected interpreter — fully offline. Also installs the free <b style="color:#bbbbbb">LSP4IJ</b> plugin (one click, may prompt a restart).</div>
        </div>
      </div>
"""

SCREENS = [
    ("01-syntax-highlighting", "greeter.rb — greeter", TREE + editor(), None),
    ("02-run-a-file", "greeter.rb — greeter", TREE + editor(console=CONSOLE), None),
    ("03-download-interpreter", "greeter.rb — greeter", TREE + editor(extra=DIALOG_DOWNLOAD), None),
    ("04-setup-banner", "greeter.rb — greeter", TREE + editor(banner=BANNER_SETUP), None),
    ("05-code-intelligence", "greeter.rb — greeter", TREE + editor(banner=BANNER_CI), None),
    ("06-settings", "Settings", SETTINGS_TREE + SETTINGS, SETTINGS_FOOTER),
    ("07-bundler-tools", "greeter.rb — greeter", TREE + editor(extra=DIALOG_CARGO), None),
]


def main():
    if not pathlib.Path(CHROME).exists():
        sys.exit(f"Chrome not found at {CHROME}")
    WORK.mkdir(exist_ok=True)
    shutil.copy(SRC / "shell.css", WORK / "shell.css")
    OUT.mkdir(parents=True, exist_ok=True)

    for name, title, inner, footer in SCREENS:
        (WORK / f"{name}.html").write_text(page(title, inner, footer))

    for name, *_ in SCREENS:
        png = OUT / f"{name}.png"
        png.unlink(missing_ok=True)
        # Not check=True: headless Chrome on macOS writes the PNG correctly but still
        # exits non-zero on harmless CVDisplayLinkCreateWithCGDisplay errors. The
        # output file is the real success signal.
        subprocess.run(
            [CHROME, "--headless", "--disable-gpu", "--hide-scrollbars",
             "--force-device-scale-factor=2", "--window-size=1200,760",
             f"--screenshot={png}", str(WORK / f"{name}.html")],
            capture_output=True,
        )
        if not png.exists() or png.stat().st_size < 10_000:
            sys.exit(f"render failed: {name}")
        print(f"  rendered {name}.png")

    shutil.rmtree(WORK, ignore_errors=True)
    print(f"\n{len(SCREENS)} screenshots -> {OUT}")


if __name__ == "__main__":
    main()
