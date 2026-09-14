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
@param int copyGroups
@param int copyLines
@param int forkGroups
@param int forkLines
@param double copyShare
@param boolean mostlyCopies
@param int minFork
@param String topPrefix
@param int topCount
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Leftover copies — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a> · <a href="/keys">Keys</a> · <a href="/forks">Forks</a> · <a href="/affix">Affix</a> · <a href="/lanes">Lanes</a> · <a href="/rows">Rows</a> · <a href="/cliff">Cliff</a> · <a href="/runway">Runway</a> · <a href="/rise">Rise</a> · <a href="/majority">Majority</a> · <a href="/near">Near</a> · <a href="/residue">Residue</a> · <a href="/linger">Linger</a></nav>
    <h1>Leftover copies</h1>
    <p>Keep newlines. DNA leftover after unique share 0.90 is all forks
        (copyShare <strong>0</strong>, minFork <strong>11</strong>). Log leftover
        often has <code>modalSplit=0</code> — identical copies — so
        <code>stretched</code> is false and not a go signal. Split leftover
        twins into <code>copyGroups</code> (dedupe) versus
        <code>forkGroups</code> (still differ). If <code>mostlyCopies</code>
        (copyShare &gt; 0.5), strip duplicates rather than walking splitLag.
        Inverse of residue stretch. Not <code>jcvi-dna.txt</code>, not a
        project file. Distinct from wrap-frame clones.</p>

    <form method="post" action="/dups">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p><button type="submit">Profile copies</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(rowCount == 0)
            <p class="muted">No non-blank lines.</p>
        @else
            <p>${lineCount} line(s), ${rowCount} record(s). nearAt
                <code>${nearAt}</code>, copy groups
                <code>${copyGroups}</code> covering <code>${copyLines}</code>
                line(s), fork groups <code>${forkGroups}</code>, copy share
                <code>${copyShare}</code>, mostly copies
                <code>${mostlyCopies}</code>, min fork
                <code>${minFork}</code>. Top copy prefix
                <code>${topPrefix}</code> × <code>${topCount}</code>.</p>
        @endif
    @endif
</body>
</html>
