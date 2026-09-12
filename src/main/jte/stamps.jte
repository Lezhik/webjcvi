@param boolean ranked
@param int k
@param int maxChars
@param String summary
@param String topKmer
@param long topCount
@param java.util.List<org.webjcvi.stamp.KmerStamp.Stamp> stamps
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>K-mer stamps — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .stamp { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/reflow">Unwrap</a> · <a href="/rare">Rare islands</a> · <a href="/tokens">Tokens</a> · <a href="/fold">Fold</a></nav>
    <h1>K-mer stamps</h1>
    <p>Overlapping k-mers on a whitespace-stripped, case-folded tape. Default
        <strong>k=3</strong> because <code>TTT</code> led all three codon frames and
        GC spread was only <strong>1.15%</strong> — the tape is phase-invariant, so
        the mode stamp is the signal, not an ORF. Inverse of rare islands: here we
        keep the <em>most common</em> short motifs (log prefixes, boilerplate).
        Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/stamps">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>k
                <input type="number" name="k" min="2" max="8" value="${k}">
            </label>
        </p>
        <p><button type="submit">Rank stamps</button></p>
    </form>

    @if(ranked)
        <p><code>${summary}</code></p>
        @if(stamps.isEmpty())
            <p class="muted">No overlapping k-mers with count ≥ 2.</p>
        @else
            <p>Top stamp <code>${topKmer}</code> × ${topCount}.</p>
            @for(org.webjcvi.stamp.KmerStamp.Stamp stamp : stamps)
                <div class="stamp">
                    <p><code>#${stamp.index()} ${stamp.kmer()} × ${stamp.count()}</code></p>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
