@param boolean scanned
@param int wrapWidth
@param int maxChars
@param String summary
@param int recordCount
@param String topRc
@param String topReverse
@param String topIdentity
@param java.util.List<org.webjcvi.frame.FrameFields.Record> records
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Frame fields — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a> · <a href="/contrast">Contrast</a> · <a href="/mirror">Mirrors</a> · <a href="/seam">Seams</a> · <a href="/phase">Phase</a></nav>
    <h1>Frame fields</h1>
    <p>Drop newlines and split the tape into frames of width <strong>70</strong>.
        Extract the three 8-character slots that the map-phase census assigned
        to different neighbor maps: RC at column <strong>4</strong> (share
        0.0111), reverse at <strong>19</strong> (0.0140), identity at
        <strong>58</strong> (0.0121). The wrap frame is a typed record, not
        one compositional column. Inverse of phase-joint Hamming. Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/fields">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Wrap width
                <input type="number" name="wrap" min="66" value="${wrapWidth}">
            </label>
        </p>
        <p><button type="submit">Extract fields</button></p>
    </form>

    @if(scanned)
        <p><code>${summary}</code></p>
        @if(records.isEmpty())
            <p class="muted">No complete wrap frames.</p>
        @else
            <p>${recordCount} record(s) (showing ${records.size()}).</p>
            <p>Mode RC <code>${topRc}</code> · reverse <code>${topReverse}</code> · identity <code>${topIdentity}</code></p>
            @for(org.webjcvi.frame.FrameFields.Record row : records)
                <div class="hit">
                    <p><code>#${row.index()} offset ${row.offset()}</code></p>
                    <pre>rc ${row.rc()}
rev ${row.reverse()}
id  ${row.identity()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
