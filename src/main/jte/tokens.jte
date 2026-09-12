@param boolean cut
@param int minToken
@param int maxChars
@param String rareClass
@param String summary
@param int tokenCount
@param java.util.List<org.webjcvi.token.RareBreakTokenizer.Token> tokens
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Rare-break tokens — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .token { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/stamps">Stamps</a> · <a href="/fold">Fold</a> · <a href="/loop">Loops</a> · <a href="/fuzzy">Fuzzy</a></nav>
    <h1>Rare-break tokens</h1>
    <p>Treats the bottom <strong>24.14%</strong> of symbol mass as <em>breaks</em>
        (DNA GC islands: longest 9, none ≥ 10) and lists the majority-class
        tokens between them (AT islands: mean ~4, longest 54). Whitespace also
        breaks. Inverse of rare-class islands: here the rare class is punctuation,
        not the payload. Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/tokens">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Minimum token length
                <input type="number" name="min" min="2" value="${minToken}">
            </label>
        </p>
        <p><button type="submit">Cut tokens</button></p>
    </form>

    @if(cut)
        <p><code>${summary}</code></p>
        @if(tokens.isEmpty())
            <p class="muted">No majority tokens of length ${minToken} or more.</p>
        @else
            <p>${tokenCount} token(s); breaks are class <code>${rareClass}</code>.</p>
            @for(org.webjcvi.token.RareBreakTokenizer.Token token : tokens)
                <div class="token">
                    <p><code>#${token.index()} offset ${token.offset()} length ${token.length()}</code></p>
                    <pre>${token.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
