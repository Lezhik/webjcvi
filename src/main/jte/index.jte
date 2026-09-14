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
        <p><a href="/contrast">Block contrast</a> (adjacent-window stutters — neighbors should differ).</p>
        <p><a href="/mirror">Mirror joints</a> (adjacent reverse blocks that are not copies).</p>
        <p><a href="/seam">Seam guard</a> (palindromic glue at hard-wrap line joints).</p>
        <p><a href="/phase">Phase joints</a> (reverse field at wrap-frame column 19).</p>
        <p><a href="/fields">Frame fields</a> (extract RC/reverse/identity slots from 70-wide records).</p>
        <p><a href="/clones">Clone frames</a> (find duplicate 70-wide wrap records).</p>
        <p><a href="/prefix">Prefix families</a> (group records by a shared leading 8-mer).</p>
        <p><a href="/keys">Key width</a> (smallest prefix that uniquely identifies every wrap-frame).</p>
        <p><a href="/forks">Near-unique forks</a> (residual twins at a 16-character fingerprint, and where they diverge).</p>
        <p><a href="/affix">Affix keys</a> (which end of a wrap-frame saturates uniqueness first).</p>
        <p><a href="/lanes">Uniqueness lanes</a> (which wrap-frame column is the cheapest 16-character fingerprint).</p>
        <p><a href="/rows">Line records</a> (uniqueness of newline-delimited lines, without dropping newlines).</p>
        <p><a href="/cliff">Clock cliff</a> (where colliding line prefixes first fork, past a timestamp).</p>
        <p><a href="/runway">Layout runway</a> (gap from first fork to uniqueness cliff — strip layout, start the id at cliffAt).</p>
        <p><a href="/rise">Uniqueness rise</a> (steepest unique-share jump, even when uniqueness never reaches 0.99).</p>
        <p><a href="/majority">Majority width</a> (smallest prefix k whose unique share is at least 0.5 — use when the steepest jump is not yet an identifier).</p>
        <p><a href="/near">Near-unique width</a> (smallest prefix k whose unique share is at least 0.90 — use when half-unique is not yet a key and 0.99 never arrives).</p>
        <p><a href="/residue">Near residue</a> (leftover colliding prefixes after unique share 0.90 — use when near-unique still leaves twins, instead of waiting for 0.99).</p>
        <p><a href="/dups">Leftover copies</a> (exact duplicate leftover lines after 0.90 versus twins that still fork — use when stretched is false because leftover is copies).</p>
        <form method="post" action="/report/regenerate">
            <button type="submit">Regenerate report</button>
        </form>
    </div>

    <p class="muted">File access is limited to this project directory and 500&nbsp;MB per file. The MCP tools <code>list_files</code> and <code>read_file</code> use the same rules.</p>
</body>
</html>
