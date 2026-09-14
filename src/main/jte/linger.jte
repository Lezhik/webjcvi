@param boolean scanned
@param int maxChars
@param String summary
@param int lineCount
@param int rowCount
@param int nearAt
@param int twinGroups
@param int lag0
@param int lag1
@param int lag2
@param int lagLong
@param int modalLag
@param int maxLag
@param double longShare
@param boolean longTail
@param double copyShare
@param boolean mostlyCopies
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Leftover linger — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a> · <a href="/keys">Keys</a> · <a href="/forks">Forks</a> · <a href="/affix">Affix</a> · <a href="/lanes">Lanes</a> · <a href="/rows">Rows</a> · <a href="/cliff">Cliff</a> · <a href="/runway">Runway</a> · <a href="/rise">Rise</a> · <a href="/majority">Majority</a> · <a href="/near">Near</a> · <a href="/residue">Residue</a> · <a href="/dups">Copies</a></nav>
    <h1>Leftover linger</h1>
    <p>Keep newlines. DNA leftover after unique share 0.90 never copies
        (lag0 <strong>0</strong>), modes at lag 1, but
        <code>lagLong=33</code> groups persist past DNA's two-base climb.
        Log leftover is often mostly copies, so
        <code>stretched</code> is false — yet some leftover twins still
        share more than two characters past <code>nearAt</code>. If
        <code>longTail</code> (<code>lagLong</code> &gt; 0), inspect lingering
        leftover even when <code>mostlyCopies</code>. Inverse of copy
        majority and of modal stretch. Not <code>jcvi-dna.txt</code>, not a
        project file.</p>

    <form method="post" action="/linger">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p><button type="submit">Profile linger</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(rowCount == 0)
            <p class="muted">No non-blank lines.</p>
        @else
            <p>${lineCount} line(s), ${rowCount} record(s). nearAt
                <code>${nearAt}</code>, twin groups
                <code>${twinGroups}</code>, lag0
                <code>${lag0}</code>, lag1
                <code>${lag1}</code>, lag2
                <code>${lag2}</code>, lagLong
                <code>${lagLong}</code>, modal lag
                <code>${modalLag}</code>, max lag
                <code>${maxLag}</code>, long share
                <code>${longShare}</code>, long tail
                <code>${longTail}</code>, mostly copies
                <code>${mostlyCopies}</code>.</p>
        @endif
    @endif
</body>
</html>
