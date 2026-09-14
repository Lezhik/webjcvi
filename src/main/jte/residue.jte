@param boolean scanned
@param int maxChars
@param String summary
@param int lineCount
@param int rowCount
@param int nearAt
@param double shareAtNear
@param double residueShare
@param int twinGroups
@param int twinLines
@param int minSplit
@param int modalSplit
@param int maxSplit
@param int splitLag
@param boolean stretched
@param String topPrefix
@param int topCount
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Near residue — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        textarea, button { font: inherit; }
        textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a> · <a href="/keys">Keys</a> · <a href="/forks">Forks</a> · <a href="/affix">Affix</a> · <a href="/lanes">Lanes</a> · <a href="/rows">Rows</a> · <a href="/cliff">Cliff</a> · <a href="/runway">Runway</a> · <a href="/rise">Rise</a> · <a href="/majority">Majority</a> · <a href="/near">Near</a></nav>
    <h1>Near residue</h1>
    <p>Keep newlines. DNA uniqueness turns near-unique at k=<strong>10</strong>
        (share 0.9532) then cliffs two bases later at k=<strong>12</strong>
        (share 0.9943). Log lines often hit unique share 0.90 with ~8% still
        colliding, and never reach 0.99. Report leftover twin groups at
        <code>nearAt</code> and where they first split. If
        <code>stretched</code> (splitLag &gt; 2), do not treat
        <code>nearAt</code> as an identifier for those leftovers — start at
        <code>modalSplit</code>. Inverse of near-unique width. Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/residue">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p><button type="submit">Profile residue</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(rowCount == 0)
            <p class="muted">No non-blank lines.</p>
        @else
            <p>${lineCount} line(s), ${rowCount} record(s). nearAt
                <code>${nearAt}</code>, share at near
                <code>${shareAtNear}</code>, residue share
                <code>${residueShare}</code>, twin groups
                <code>${twinGroups}</code> covering <code>${twinLines}</code>
                line(s). modal split <code>${modalSplit}</code>, split lag
                <code>${splitLag}</code>, stretched
                <code>${stretched}</code>. Top leftover prefix
                <code>${topPrefix}</code> × <code>${topCount}</code>.</p>
        @endif
    @endif
</body>
</html>
