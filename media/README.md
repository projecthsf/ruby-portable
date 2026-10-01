# Marketplace media

`marketplace/*.png` are the screenshots for the JetBrains Marketplace listing, at the same
2400×1520 the other Portable plugins use.

## Regenerating

```bash
cd media/src && python3 build.py
```

One 1200×760 HTML page per screen, rendered by headless Chrome at
`--force-device-scale-factor=2`. `shell.css` holds the shared IDE chrome and the Darcula palette.

Chrome on macOS **writes the PNG correctly but exits non-zero** on harmless
`CVDisplayLinkCreateWithCGDisplay` errors, so the script checks the output file rather than the
exit code. Don't "fix" that with `check=True`.

## The palette is read, not guessed

Every colour in `shell.css` is the Darcula scheme's own value, extracted from the IDE
distribution rather than eyeballed:

```bash
unzip -p <ide>/lib/app-client.jar DefaultColorSchemesManager.xml > schemes.xml
# then read the <scheme name="Darcula"> block; FONT_TYPE 2 means italic
```

| Key | Darcula | Used for |
|---|---|---|
| `DEFAULT_KEYWORD` | `#CC7832` | keywords |
| `DEFAULT_STRING` | `#6A8759` | strings, heredocs, `%w`, regexes |
| `DEFAULT_NUMBER` | `#6897BB` | numbers |
| `DEFAULT_LINE_COMMENT` | `#808080` | comments |
| `DEFAULT_INSTANCE_FIELD` | `#9876AA` | symbols, `@ivar`, `@@cvar` |
| `DEFAULT_CONSTANT` | `#9876AA` *italic* | constants and class names |
| `DEFAULT_FUNCTION_DECLARATION` | `#FFC66D` | method names after `def` |
| `DEFAULT_IDENTIFIER` | `#A9B7C6` | everything else |

Worth knowing: **`DEFAULT_GLOBAL_VARIABLE`, `DEFAULT_LABEL` and `DEFAULT_FUNCTION_CALL` are not
overridden by Darcula**, so anything mapped to them renders as plain identifier colour. That is
why `$globals` get no distinct colour here, and it is a real finding for the sibling plugins too —
rust-portable maps lifetimes to `LABEL` and macros to `FUNCTION_CALL`, so both render plain.

## Keeping the listing honest

Every string in these renders is copied from the plugin's own source — banner text, button
labels, dialog titles, the interpreter list, the run output. **If you change UI copy, change it
here and re-render.** The run-marker position matters too: `RubyRunLineMarkerContributor` anchors
on the file's *first* leaf, so the ▶ belongs on line 1, not on the first `class`.

## What is deliberately missing

No completion / go-to-definition / hover / diagnostics screens. Those are ruby-lsp features and
had not been exercised end-to-end when these were made — only the interpreter download, the run
path, both editor banners and the settings panel were confirmed in a sandbox IDE. Add them once
ruby-lsp has actually been seen running, not before.
