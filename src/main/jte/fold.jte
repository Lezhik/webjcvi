@param boolean found
@param int minLength
@param int maxChars
@param String summary
@param int hitCount
@param int longest
@param java.util.List<org.webjcvi.fold.PalindromeScan.Hit> hits
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Palindromes — WebJCVI</title>
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
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/stamps">Stamps</a></nav>
    <h1>Fold palindromes</h1>
    <p>Finds even palindromes of length ≥ <strong>4</strong> that are not
        homopolymers. Forward same-base identity is nearly chance (lag 1 ×
        <strong>1.134</strong>) and lag 2 is depleted (× <strong>0.944</strong>),
        so XYX 3-mers are skipped; Chargaff A≈T is treated as a <em>fold</em>,
        not a forward lag. Whitespace stripped, case folded.
        Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/fold">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Minimum length
                <input type="number" name="min" min="4" value="${minLength}">
            </label>
        </p>
        <p><button type="submit">Find palindromes</button></p>
    </form>

    @if(found)
        <p><code>${summary}</code></p>
        @if(hits.isEmpty())
            <p class="muted">No even palindromes of length ${minLength} or more (homopolymers omitted).</p>
        @else
            <p>${hitCount} palindrome(s), longest ${longest}.</p>
            @for(org.webjcvi.fold.PalindromeScan.Hit hit : hits)
                <div class="hit">
                    <p><code>#${hit.index()} offset ${hit.offset()} length ${hit.length()}</code></p>
                    <pre>${hit.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
