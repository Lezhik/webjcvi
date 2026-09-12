@param int length
@param String preview
@param String query
@param boolean searched
@param java.util.List<org.webjcvi.tape.ScratchTape.Hit> hits
@param java.util.List<org.webjcvi.tape.ScratchTape.Run> runs
@param int minRun
@param int maxChars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Scratch tape — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=text], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 8rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .hit, .run { margin: 0.5rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a></nav>
    <h1>Scratch tape</h1>
    <p>Caller-supplied text only — not <code>jcvi-dna.txt</code>, not a project file.
        Search is fold-then-exact. Runs of identical characters are listed the same way the DNA preview showed poly-A/T.</p>

    <form method="post" action="/tape/load">
        <p>
            <label>Load text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p><button type="submit">Load tape</button></p>
    </form>

    <p>Tape length: <strong>${length}</strong></p>
    @if(length > 0)
        <pre>${preview}</pre>
    @endif

    <form method="get" action="/tape">
        <p>
            <label>Motif <input type="text" name="q" value="${query}"></label>
            <button type="submit">Find</button>
        </p>
    </form>

    @if(searched)
        @if(hits.isEmpty())
            <p class="muted">No hits for <code>${query}</code>.</p>
        @else
            @for(org.webjcvi.tape.ScratchTape.Hit hit : hits)
                <div class="hit"><code>line ${hit.line()} offset ${hit.offset()}</code> — ${hit.snippet()}</div>
            @endfor
        @endif
    @endif

    <h2>Runs of length ≥ ${minRun}</h2>
    @if(runs.isEmpty())
        <p class="muted">No runs (empty tape or none long enough).</p>
    @else
        @for(org.webjcvi.tape.ScratchTape.Run run : runs)
            <div class="run"><code>${run.symbol()}</code> × ${run.length()} at offset ${run.offset()}</div>
        @endfor
    @endif
</body>
</html>
