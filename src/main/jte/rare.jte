@param boolean scanned
@param int minIsland
@param int maxChars
@param String rareClass
@param String summary
@param int islandCount
@param java.util.List<org.webjcvi.rare.RareClassScanner.Island> islands
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Rare islands — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .island { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a></nav>
    <h1>Rare-class islands</h1>
    <p>Takes symbols from the rarest upward until they cover about <strong>24.14%</strong>
        of non-whitespace mass (the DNA GC share). Majority symbols — those above
        <strong>75.86%</strong> — break islands, the way AT frames GC. Lists consecutive
        runs of the rare class: hex blobs, IDs, punctuation clusters in logs.
        Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/rare">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Minimum island length
                <input type="number" name="min" min="2" value="${minIsland}">
            </label>
        </p>
        <p><button type="submit">Find islands</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(islands.isEmpty())
            <p class="muted">No rare-class islands of length ${minIsland} or more.</p>
        @else
            <p>${islandCount} island(s) in class <code>${rareClass}</code>.</p>
            @for(org.webjcvi.rare.RareClassScanner.Island island : islands)
                <div class="island">
                    <p><code>#${island.index()} offset ${island.offset()} length ${island.length()}</code></p>
                    <pre>${island.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
