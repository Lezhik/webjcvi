@param boolean scanned
@param int width
@param int maxChars
@param String summary
@param int jointCount
@param java.util.List<org.webjcvi.mirror.MirrorJoint.Hit> hits
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Mirror joints — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a></nav>
    <h1>Mirror joints</h1>
    <p>Walk overlapping windows of width <strong>4</strong> and flag the next
        block when it is the <em>reverse</em> of the current one, but not a
        copy. Neighbor Hamming: reverse distance 0 is
        <em>enriched</em> (×<strong>1.130</strong>) while identity is
        depleted (×<strong>0.891</strong>) and RC is depleted
        (×<strong>0.846</strong>) — the seam is a mirror, not a stutter.
        Inverse of block contrast. Not <code>jcvi-dna.txt</code>, not a
        project file.</p>

    <form method="post" action="/mirror">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Window width
                <input type="number" name="width" min="2" value="${width}">
            </label>
        </p>
        <p><button type="submit">Find mirrors</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(hits.isEmpty())
            <p class="muted">No reverse joints that are not copies.</p>
        @else
            <p>${jointCount} mirror joint(s) (showing ${hits.size()}).</p>
            @for(org.webjcvi.mirror.MirrorJoint.Hit hit : hits)
                <div class="hit">
                    <p><code>#${hit.index()} offset ${hit.offset()} identity ${hit.identityDistance()}</code></p>
                    <pre>${hit.left()} | ${hit.right()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
