@param boolean scanned
@param int maxChars
@param String summary
@param int lineCount
@param int rowCount
@param int nearAt
@param double shareAtNear
@param int majorityAt
@param double shareAtMajority
@param int lag
@param boolean pastMajority
@param String topPrefix
@param int topCount
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Near-unique width — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a> · <a href="/keys">Keys</a> · <a href="/forks">Forks</a> · <a href="/affix">Affix</a> · <a href="/lanes">Lanes</a> · <a href="/rows">Rows</a> · <a href="/cliff">Cliff</a> · <a href="/runway">Runway</a> · <a href="/rise">Rise</a> · <a href="/majority">Majority</a></nav>
    <h1>Near-unique width</h1>
    <p>Keep newlines. DNA uniqueness covers a majority at the floor (share 0.7191)
        then reaches 0.8726 at k=<strong>9</strong> and cliffs at k=<strong>12</strong>
        (share 0.9943). Log lines often hit unique share 0.5 far from a near-unique
        key, and never reach 0.99. Report <code>nearAt</code> — the smallest prefix
        length whose unique share is ≥ 0.90. If <code>pastMajority</code>
        (nearAt &gt; majorityAt), start an identifier at <code>nearAt</code>, not at
        the half-unique majority. Inverse of majority-width. Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/near">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p><button type="submit">Profile near</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(rowCount == 0)
            <p class="muted">No non-blank lines.</p>
        @else
            <p>${lineCount} line(s), ${rowCount} record(s). nearAt
                <code>${nearAt}</code>, share at near
                <code>${shareAtNear}</code>, majorityAt <code>${majorityAt}</code>,
                past majority <code>${pastMajority}</code>, lag <code>${lag}</code>.
                Top prefix <code>${topPrefix}</code> × <code>${topCount}</code>.</p>
        @endif
    @endif
</body>
</html>
