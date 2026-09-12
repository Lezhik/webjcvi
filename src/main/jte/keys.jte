@param boolean scanned
@param int wrapWidth
@param int maxChars
@param String summary
@param int frameCount
@param int uniqueAt
@param double uniqueShareAtFloor
@param double uniqueShareAtWrap
@param java.util.List<org.webjcvi.key.KeyWidth.Hit> samples
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Key width — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .hit { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a></nav>
    <h1>Key width</h1>
    <p>Drop newlines and split the tape into frames of width <strong>70</strong>.
        Prefix length <strong>8</strong> still collides (DNA unique share 0.719)
        while the whole 70-mer is unique. Shared headers are a key that is too
        short, not a family catalog. Report the smallest prefix length at which
        every record is unique. Inverse of prefix families. Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/keys">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Wrap width
                <input type="number" name="wrap" min="8" value="${wrapWidth}">
            </label>
        </p>
        <p><button type="submit">Measure key width</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(frameCount == 0)
            <p class="muted">No wrap frames.</p>
        @else
            <p>${frameCount} frame(s). Unique at <code>k=${uniqueAt}</code>
                (0 means never unique). Share at floor 8:
                <code>${uniqueShareAtFloor}</code>; at wrap:
                <code>${uniqueShareAtWrap}</code>.</p>
            @for(org.webjcvi.key.KeyWidth.Hit hit : samples)
                <div class="hit">
                    <p><code>k ${hit.length()} distinct ${hit.distinct()} families ${hit.familyCount()} uniqueShare ${hit.uniqueShare()}</code></p>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
