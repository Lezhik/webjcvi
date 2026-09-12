@param boolean scanned
@param int wrapWidth
@param int blockWidth
@param int maxChars
@param String summary
@param int hitCount
@param java.util.List<org.webjcvi.seam.SeamGuard.Hit> hits
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Seam guard — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a></nav>
    <h1>Seam guard</h1>
    <p>Inspect newline joints of lines at least <strong>70</strong> characters
        (the modal FASTA wrap). Flag the seam when the last
        <strong>4</strong> characters of a line are the reverse of the first
        4 of the next, but not a copy. Wrap reverse-0 share is
        <em>depleted</em> vs the linear interior (wrap/interior
        <strong>0.913</strong>) — palindromic glue is the anomaly, not the
        protocol. Inverse of linear mirror joints. Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/seam">
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
        <p>
            <label>Block width
                <input type="number" name="block" min="2" value="${blockWidth}">
            </label>
        </p>
        <p><button type="submit">Flag seams</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(hits.isEmpty())
            <p class="muted">No palindromic wrap seams.</p>
        @else
            <p>${hitCount} palindromic seam(s) (showing ${hits.size()}).</p>
            @for(org.webjcvi.seam.SeamGuard.Hit hit : hits)
                <div class="hit">
                    <p><code>#${hit.index()} line ${hit.line()} identity ${hit.identityDistance()}</code></p>
                    <pre>${hit.left()} | ${hit.right()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
