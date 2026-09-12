@param boolean scanned
@param int wrapWidth
@param int phase
@param int blockWidth
@param int maxChars
@param String summary
@param int hitCount
@param java.util.List<org.webjcvi.phase.PhaseJoint.Hit> hits
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phase joints — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a></nav>
    <h1>Phase joints</h1>
    <p>Walk the linear tape (newlines dropped) in frames of width
        <strong>70</strong> and inspect only column <strong>19</strong> —
        the reverse-0 peak (share <strong>0.0140</strong> vs mean
        <strong>0.0114</strong>). Flag when the next 4 characters are the
        reverse of the current 4, but not a copy. Wrap column 66 is depleted
        (wrap/mean <strong>0.913</strong>), so this is an interior field,
        not a seam. Inverse of seam guard. Not <code>jcvi-dna.txt</code>,
        not a project file.</p>

    <form method="post" action="/phase">
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
            <label>Phase column
                <input type="number" name="phase" min="0" value="${phase}">
            </label>
        </p>
        <p>
            <label>Block width
                <input type="number" name="block" min="2" value="${blockWidth}">
            </label>
        </p>
        <p><button type="submit">Find phase joints</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(hits.isEmpty())
            <p class="muted">No reverse joints at this phase.</p>
        @else
            <p>${hitCount} phase joint(s) (showing ${hits.size()}).</p>
            @for(org.webjcvi.phase.PhaseJoint.Hit hit : hits)
                <div class="hit">
                    <p><code>#${hit.index()} offset ${hit.offset()} identity ${hit.identityDistance()}</code></p>
                    <pre>${hit.left()} | ${hit.right()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
