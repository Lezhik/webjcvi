@param boolean split
@param int minRun
@param int maxChars
@param int sectionCount
@param java.util.List<org.webjcvi.segment.BannerSplitter.Section> sections
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Banner split — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        input, textarea, button { font: inherit; }
        input[type=number], textarea { width: min(40rem, 100%); padding: 0.35rem 0.5rem; }
        textarea { min-height: 10rem; }
        button { padding: 0.4rem 0.9rem; cursor: pointer; }
        code { font-family: ui-monospace, Consolas, monospace; }
        .section { border: 1px solid currentColor; padding: 0.75rem 1rem; margin: 0.75rem 0; }
        .muted { opacity: 0.75; }
        nav { margin-bottom: 1.5rem; }
        pre { white-space: pre-wrap; word-break: break-word; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a> · <a href="/tape">Scratch tape</a></nav>
    <h1>Banner split</h1>
    <p>Splits caller-supplied text on unusually long identical-character runs
        (log banners such as <code>==========</code> or <code>----------</code>).
        Default minimum run length is <strong>10</strong> — the rare class in the DNA homopolymer census.
        Not <code>jcvi-dna.txt</code>, not a project file.</p>

    <form method="post" action="/split">
        <p>
            <label>Text (max ${maxChars} characters)<br>
                <textarea name="text" required></textarea>
            </label>
        </p>
        <p>
            <label>Minimum banner length
                <input type="number" name="minRun" min="2" value="${minRun}">
            </label>
        </p>
        <p><button type="submit">Split sections</button></p>
    </form>

    @if(split)
        <h2>${sectionCount} section(s)</h2>
        @if(sections.isEmpty())
            <p class="muted">No sections.</p>
        @else
            @for(org.webjcvi.segment.BannerSplitter.Section section : sections)
                <div class="section">
                    <p><code>#${section.index()} offset ${section.offset()} length ${section.length()}</code></p>
                    @if(!section.banner().isEmpty())
                        <p class="muted">After banner <code>${section.banner()}</code></p>
                    @endif
                    <pre>${section.preview()}</pre>
                </div>
            @endfor
        @endif
    @endif
</body>
</html>
