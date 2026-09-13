@param boolean scanned
@param int maxChars
@param String summary
@param int lineCount
@param int rowCount
@param int forkAt
@param int cliffAt
@param double shareAt16
@param double shareAtCliff
@param String topPrefix
@param int topCount
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Clock cliff — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a> · <a href="/keys">Keys</a> · <a href="/forks">Forks</a> · <a href="/affix">Affix</a> · <a href="/lanes">Lanes</a> · <a href="/rows">Rows</a></nav>
    <h1>Clock cliff</h1>
    <p>Keep newlines. DNA line uniqueness jumps from k=8 share <strong>0.7191</strong>
        to k=16 <strong>0.9997</strong> (uniqueAt 20). Log lines that share a
        16-character clock never saturate. Report <code>forkAt</code> (first
        column where the largest colliding prefix splits) and
        <code>cliffAt</code> (smallest k with unique share ≥ 0.99). Inverse of
        line records (there uniqueShareAt16 was the veto). Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/cliff">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p><button type="submit">Profile cliff</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(rowCount == 0)
            <p class="muted">No non-blank lines.</p>
        @else
            <p>${lineCount} line(s), ${rowCount} record(s). forkAt
                <code>${forkAt}</code>, cliffAt <code>${cliffAt}</code>,
                share at 16 <code>${shareAt16}</code>, share at cliff
                <code>${shareAtCliff}</code>. Top prefix
                <code>${topPrefix}</code> × <code>${topCount}</code>.</p>
        @endif
    @endif
</body>
</html>
