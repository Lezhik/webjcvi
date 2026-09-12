@param boolean searched
@param int maxDist
@param String motif
@param int maxChars
@param int minMotif
@param String summary
@param int hitCount
@param java.util.List<org.webjcvi.fuzzy.FuzzyFind.Hit> hits
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Fuzzy find — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], input[type=text], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a></nav>
    <h1>Fuzzy find</h1>
    <p>Sliding Hamming search for a motif with up to <strong>1</strong> substitution
        by default. Hairpin stems were <em>flat</em> across loop widths 0–13
        (~<strong>44k–55k</strong>), so exact complementary pairing is background,
        not a protocol. Min motif <strong>${minMotif}</strong> is the hairpin stem
        floor. Inverse of exact tape find. Not <code>jcvi-dna.txt</code>, not a
        project file.</p>

    <form method="post" action="/fuzzy">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Motif (min ${minMotif})<br>
                <input type="text" name="motif" required>
            </label>
        </p>
        <p>
            <label>Max Hamming distance
                <input type="number" name="dist" min="0" value="${maxDist}">
            </label>
        </p>
        <p><button type="submit">Find near matches</button></p>
    </form>

    @if(searched)
        <p><code>${summary}</code></p>
        @if(hits.isEmpty())
            <p class="muted">No windows within distance ${maxDist} of <code>${motif}</code>.</p>
        @else
            <p>${hitCount} hit(s).</p>
            @for(org.webjcvi.fuzzy.FuzzyFind.Hit hit : hits)
                <div class="hit">
                    <p><code>#${hit.index()} offset ${hit.offset()} distance ${hit.distance()}</code></p>
                    <pre>${hit.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
