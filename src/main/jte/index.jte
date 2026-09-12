@param boolean hasReport
@param int length
@param String gcPercent
@param long ambiguous
@param long invalid
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        .panel { border: 1px solid currentColor; padding: 1rem 1.25rem; margin: 1.25rem 0; }
        button { font: inherit; padding: 0.4rem 0.9rem; cursor: pointer; }
        .muted { opacity: 0.75; }
        code { font-family: ui-monospace, Consolas, monospace; }
    </style>
</head>
<body>
    <h1>WebJCVI</h1>
    <p>A dual-surface app: this page for humans, and an MCP server for agents. Both call the same sandboxed storage and DNA report classes.</p>

    <div class="panel">
        @if(hasReport)
            <p>Current report: <strong>${length}</strong> normalized bases,
                GC <strong>${gcPercent}%</strong>,
                ambiguous <strong>${ambiguous}</strong>,
                invalid <strong>${invalid}</strong>.</p>
            <p><a href="/report">Open the full report</a></p>
        @else
            <p>No report on disk yet. Generate one from <code>jcvi-dna.txt</code>.</p>
            @endif
        <p><a href="/tape">Open the scratch tape</a> (paste text, exact-find, list runs).</p>
        <p><a href="/split">Open banner split</a> (cut pasted logs on long character runs).</p>
        <p><a href="/drift">Open pair drift</a> (find local bracket imbalance in 70-char windows).</p>
        <p><a href="/reflow">Unwrap hard wrap</a> (join lines of width 70 into paragraphs).</p>
        <p><a href="/rare">Rare-class islands</a> (list hex, IDs, punctuation clusters in the bottom 24% of symbol mass).</p>
        <p><a href="/tokens">Rare-break tokens</a> (split on the rare class, keep the majority words).</p>
        <p><a href="/stamps">K-mer stamps</a> (rank overlapping 3-mers — the phase-invariant mode of a paste).</p>
        <p><a href="/fold">Fold palindromes</a> (even mirrored spans, not homopolymers).</p>
        <p><a href="/loop">Stem loops</a> (extract complementary bracket spans and the enclosed loop).</p>
        <p><a href="/fuzzy">Fuzzy find</a> (Hamming-near motif search — exact pairing is background).</p>
        <form method="post" action="/report/regenerate">
            <button type="submit">Regenerate report</button>
        </form>
    </div>

    <p class="muted">File access is limited to this project directory and 500&nbsp;MB per file. The MCP tools <code>list_files</code> and <code>read_file</code> use the same rules.</p>
</body>
</html>
