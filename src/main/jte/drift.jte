@param boolean scanned
@param int window
@param int maxChars
@param String summary
@param int hotspotCount
@param int opens
@param int closes
@param String globalSkew
@param java.util.List<org.webjcvi.drift.PairDrift.Hotspot> hotspots
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Pair drift — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a></nav>
    <h1>Pair drift</h1>
    <p>Walks caller-supplied text in windows of width <strong>70</strong> (the DNA wrap frame)
        and flags spans where complementary brackets <code>()[]{}&lt;&gt;</code> locally unbalance —
        the analog of 80% of DNA frames failing Chargaff while the whole tape still sums A≈T.
        Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/drift">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Window
                <input type="number" name="window" min="2" value="${window}">
            </label>
        </p>
        <p><button type="submit">Scan drift</button></p>
    </form>

    @if(scanned)
        <p>Global leftover: <code>${opens}</code> opens, <code>${closes}</code> closes,
            skew <strong>${globalSkew}</strong>. ${hotspotCount} hotspot(s).
            <span class="muted">${summary}</span></p>
        @if(hotspots.isEmpty())
            <p class="muted">No local hotspots (no brackets, or every window stays within 0.05).</p>
        @else
            @for(org.webjcvi.drift.PairDrift.Hotspot hit : hotspots)
                <div class="hit">
                    <p><code>#${hit.index()} offset ${hit.offset()} opens ${hit.opens()} closes ${hit.closes()} skew ${hit.skewText()}</code></p>
                    <pre>${hit.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
