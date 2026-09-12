@param boolean unwrapped
@param int width
@param int maxChars
@param int stitches
@param int paragraphCount
@param int sourceLines
@param java.util.List<org.webjcvi.reflow.WrapReflow.Paragraph> paragraphs
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Unwrap — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .para { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a> · <a href="/split">Banner split</a> · <a href="/drift">Pair drift</a> · <a href="/rare">Rare islands</a></nav>
    <h1>Unwrap hard wrap</h1>
    <p>Joins consecutive lines whose length is at least the DNA wrap frame
        (<strong>70</strong>). A shorter line ends the paragraph — the analog of FASTA
        wrapping a linear tape, where joints are continuation dimers (<code>TT</code>/<code>AA</code>),
        not complementary pair-closes. Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/reflow">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Wrap width
                <input type="number" name="width" min="8" value="${width}">
            </label>
        </p>
        <p><button type="submit">Unwrap</button></p>
    </form>

    @if(unwrapped)
        <p>${paragraphCount} paragraph(s) from ${sourceLines} line(s), ${stitches} stitch(es).</p>
        @if(paragraphs.isEmpty())
            <p class="muted">No paragraphs.</p>
        @else
            @for(org.webjcvi.reflow.WrapReflow.Paragraph para : paragraphs)
                <div class="para">
                    <p><code>#${para.index()} length ${para.length()}</code></p>
                    <pre>${para.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
