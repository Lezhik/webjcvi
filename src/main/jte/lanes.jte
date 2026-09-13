@param boolean scanned
@param int wrapWidth
@param int maxChars
@param String summary
@param int frameCount
@param int troughAt
@param int peakAt
@param double troughShare
@param double peakShare
@param double spread
@param java.util.List<org.webjcvi.lane.LaneScan.Lane> hits
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Uniqueness lanes — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a> · <a href="/fields">Fields</a> · <a href="/clones">Clones</a> · <a href="/prefix">Prefixes</a> · <a href="/keys">Keys</a> · <a href="/forks">Forks</a> · <a href="/affix">Affix</a></nav>
    <h1>Uniqueness lanes</h1>
    <p>Drop newlines and split the tape into frames of width <strong>70</strong>.
        DNA centered uniqueAt is <strong>17</strong> with k=16 share 0.9999 —
        the same curve as the tail (17) and lead (20). Uniqueness is not an
        edge property. Profile 16-character tiles at columns 0, 8, 19, 27,
        and 54; fingerprint at <code>peakAt</code>, avoid
        <code>troughAt</code>. Inverse of affix keys and forks.
        Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/lanes">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Wrap width
                <input type="number" name="wrap" min="16" value="${wrapWidth}">
            </label>
        </p>
        <p><button type="submit">Profile lanes</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(frameCount == 0)
            <p class="muted">No wrap frames.</p>
        @else
            <p>${frameCount} frame(s). Peak column <code>${peakAt}</code>
                (share ${peakShare}); trough <code>${troughAt}</code>
                (share ${troughShare}); spread <code>${spread}</code>.</p>
            @for(var hit : hits)
                <div class="hit">
                    <p>start <code>${hit.start()}</code> distinct
                        <code>${hit.distinct()}</code> uniqueShare
                        <code>${hit.uniqueShare()}</code></p>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
