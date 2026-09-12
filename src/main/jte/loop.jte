@param boolean extracted
@param int minLoop
@param int maxChars
@param String summary
@param int spanCount
@param int nested
@param int longestLoop
@param java.util.List<org.webjcvi.loop.StemLoop.Span> spans
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Stem loops — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .span { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a></nav>
    <h1>Stem loops</h1>
    <p>Extracts complementary delimiter spans <code>()[]{}&lt;&gt;</code> and the
        loop they enclose. Reverse-complement foldback beat letter palindromes
        (longest <strong>34</strong> vs <strong>24</strong>; <strong>96208</strong>
        vs <strong>40569</strong>) and odd RC outran even
        (<strong>51553</strong> vs <strong>44655</strong>), so the protocol is
        pairing around a gap, not a solid palindrome. Quotes are skipped (same-base
        analog). Default min loop <strong>1</strong>. Not
        <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/loop">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Minimum loop
                <input type="number" name="min" min="1" value="${minLoop}">
            </label>
        </p>
        <p><button type="submit">Extract spans</button></p>
    </form>

    @if(extracted)
        <p><code>${summary}</code></p>
        @if(spans.isEmpty())
            <p class="muted">No complementary spans with loop ≥ ${minLoop}.</p>
        @else
            <p>${spanCount} span(s), ${nested} nested, longest loop ${longestLoop}.</p>
            @for(org.webjcvi.loop.StemLoop.Span span : spans)
                <div class="span">
                    <p><code>#${span.index()} ${span.opener()}…${span.closer()} offset ${span.offset()} loop ${span.loopLength()}</code></p>
                    <pre>${span.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
