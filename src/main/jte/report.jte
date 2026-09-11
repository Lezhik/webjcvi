@param boolean hasReport
@param String markdown
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>DNA report — WebJCVI</title>
    <style>
        :root { color-scheme: light dark; }
        body { font-family: Georgia, serif; max-width: 52rem; margin: 2rem auto; padding: 0 1.25rem; line-height: 1.5; }
        a { color: inherit; }
        pre { overflow: auto; padding: 1rem; border: 1px solid currentColor; white-space: pre-wrap; word-break: break-word; }
        button { font: inherit; padding: 0.4rem 0.9rem; cursor: pointer; }
        nav { margin-bottom: 1.5rem; }
    </style>
</head>
<body>
    <nav><a href="/">← Home</a></nav>
    <h1>DNA report</h1>
    @if(hasReport)
        <form method="post" action="/report/regenerate">
            <button type="submit">Regenerate</button>
        </form>
        <pre>${markdown}</pre>
    @else
        <p>No report has been generated yet.</p>
        <form method="post" action="/report/regenerate">
            <button type="submit">Generate now</button>
        </form>
    @endif
</body>
</html>
