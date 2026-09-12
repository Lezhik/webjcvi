# AGENTS.md — WebJCVI

This file tells any AI coding agent (Cursor, Claude Code, etc.) working in this repository
how to operate. Read this file first, before touching any code. The full specification is
in `docs/tz.md` — this file is the operating procedure, `docs/tz.md` is the source of truth
for requirements if the two ever conflict.

## What this project is

WebJCVI grows an application, iteration by iteration, out of a real DNA sequence
(`jcvi-dna.txt`). The working premise: **the DNA hides rules for a system that does some
generally useful work, unrelated to the DNA itself and unrelated to this project.** Nobody
knows what that work is yet — you're expected to figure it out gradually, iteration by
iteration, by proposing a hypothesis about what the DNA encodes, building the functionality
that hypothesis implies, and honestly judging whether it's actually useful. Full mechanics
in `docs/tz.md` §1.1 and §6 — read those before running your first iteration, this is not
optional context.

This means the codebase's functionality is expected to **swing meaningfully between
iterations**, not just accumulate features on one fixed theory. That's normal. Don't try to
preserve a hypothesis just because it's already implemented — if the pros/cons analysis says
it's weak, say so and let the next hypothesis move away from it.

**Log-analysis usefulness is judged from the app's own logs.** Algorithms write DEBUG
journals; the test suite persists `reports/logs/test.log`; the log-analysis facade writes
`reports/logs/report.json`; **Pros/Cons in `docs/log.md` that concern log analysis must be
derived from that JSON**, not from speculation. See `docs/tz.md` §5.6–5.7 and §6.7–6.9.

**The app has two surfaces: a web application, and an MCP server.** It is not "a web app
with MCP bolted on" — treat both as first-class. Every capability you add should live in a
shared underlying class and be exposed through whichever surface(s) make sense; growth
decisions each iteration should explicitly consider both, not default to web-only. See
`docs/tz.md` §5.5 and §6.5.

## Tech stack — do not deviate

- **Language / framework:** Java, Spring Boot, Spring WebFlux (reactive). Do not introduce
  Spring MVC (servlet stack), blocking I/O in reactive chains, or a different web framework.
- **Templating:** JTE for any rendered HTML/report views. Do not introduce Thymeleaf,
  Freemarker, JSP, or a frontend framework unless the spec is explicitly amended.
- **MCP server:** the app also exposes an MCP server surface (`docs/tz.md` §5.5). Build MCP
  tools/resources on the same shared classes as the web UI — never duplicate logic
  between a web controller and an MCP tool handler. If you pick an MCP library/SDK, record
  the choice in `docs/log.md`.
- **Database:** none. This project is file-based only. Do not add a DB, embedded or
  otherwise, without an explicit spec change.
- **Build tool:** Gradle with the Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`).
  Do not add a `pom.xml` or switch to Groovy DSL.
- **Module layout:** single module today. Only split into a Gradle multi-project layout
  when the module-splitting trigger in `docs/tz.md` (Section 6.14) is actually reached, and
  record that decision in the log when you do it.

## Hard architectural rules

1. **All filesystem access goes through one class with a fixed API** (the file storage
   service described in `docs/tz.md` §5.1). No other class reads or writes files directly.
   If you're about to write `Files.write(...)` or `new FileInputStream(...)` outside that
   class, stop — route it through the storage service instead. (The test DEBUG journal is
   written by Logback during Gradle tests, not by application code.)
2. **The app may only read/write inside its own project directory.** Reject `..`, absolute
   paths, and symlink escapes. This must be enforced in code, not just assumed — and it must
   have a test.
3. **500 MB max file size**, enforced on both read and write, in the storage service.
4. **DNA reading and report building goes through one class with a fixed API** (`docs/tz.md`
   §5.2). The *content* of the report — and the report-building logic itself — evolves every
   iteration as part of the decryption process (§1.1, §6.10): expect to replace it with a
   meaningfully different version each time, not just tweak it. The *class API* around it
   should stay stable so other components can depend on it.
5. **DNA reports live in `build/reports/`** and are regenerated from scratch at the start of
   every iteration (old reports for the current scope are cleared first). **Log-analysis
   artifacts live in `reports/logs/`** (`test.log`, `report.json`). Both trees must stay in
   `.gitignore` — never commit generated reports or journals.
6. Keep each iteration's code change **small and reviewable** — one coherent step of growth,
   not a rewrite or a large multi-feature commit.
7. **The MCP surface must never bypass the sandboxing/size-limit rules from #1–#3.** An MCP
   tool that reads or writes files still goes through the storage service, still respects
   the project-root sandbox, and still respects the 500 MB limit — same as the web side.
8. **Log analysis goes through one facade with a fixed API** (`docs/tz.md` §5.6): input is
   text, output is JSON. Web, MCP, and the iteration Gradle task must all call that class.
   Do not reimplement analysis in a controller or tool handler.
9. **Every text-processing algorithm logs at DEBUG**, wrapped in
   `if (log.isDebugEnabled()) { log.debug(...); }`. Do not build log messages when DEBUG
   is off.

## The iteration algorithm — follow every step, in order, every time

When asked to run an iteration (or when picking up work in this repo), do exactly this. This
is the actual research method of the project — the hypothesis/analysis steps are not
bookkeeping, don't shortcut them.

1. **Regenerate the DNA report.** Call the fixed-API DNA/report class to rebuild the report
   for the current scope (whole app, or the relevant module/sub-module), using the report
   builder version currently in place, from `jcvi-dna.txt` and the code as it stands after
   the previous iteration.
2. **Read `docs/tz.md`** in full (or re-confirm you have it fresh in context).
3. **Read `docs/log.md`** in full — every prior hypothesis and its pros/cons, not just the
   most recent entry. You need this to satisfy step 4's novelty requirement.
4. **Form a hypothesis** about what generally useful system the DNA might be encoding rules
   for, and what it should therefore be able to do. It must be **meaningfully different from
   every prior hypothesis** in the log — check before you commit to one. Refining a prior
   hypothesis is fine; restating it is not.
5. **Change the web and MCP functionality** to build out what this hypothesis implies.
   Explicitly consider whether it belongs on the web UI, the MCP server, or both — don't
   default to web-only. Keep the change small and reviewable.
6. **Analyze the resulting code honestly.** Would this capability be genuinely useful on its
   own merits, independent of it coming from DNA or from this project? Say so plainly even
   if the answer is "not really" — that's a valid and useful finding, not a bad outcome to
   avoid.
7. **Run the full automated test suite** so DEBUG logs are written to `reports/logs/test.log`.
   A failing suite is not a valid stopping point.
8. **Call the log-analysis facade** on that journal and write `reports/logs/report.json`
   (Gradle `analyzeLogs`, or the same facade through `FileStorageService`). Do this after
   the DNA report (step 1) and the journal (step 7), and **before** pros/cons.
9. **Read `reports/logs/report.json`**, then determine pros and cons of the current DNA
   decryption. Log-analysis usefulness must come from that JSON (what the algorithms
   actually found), combined with the hypothesis (step 4) and the code analysis (step 6).
10. **Build a new DNA report builder version**, driven by the pros/cons from step 9, aimed
    at resolving the contradictions just identified. It must be **meaningfully different from
    every previous report builder version** — a genuinely different way of reading the same
    DNA, not a tweak. This is what step 1 will use next iteration.
11. **Append to `docs/log.md`** using exactly this structure — do not omit any field:
    ```
    ## Iteration #<index>
    - **Hypothesis:** <from step 4>
    - **Functionality changes:** <from step 5, and which surface(s)>
    - **Pros:** <from step 9, grounded in reports/logs/report.json>
    - **Cons:** <from step 9, grounded in reports/logs/report.json>
    - **Report builder changes:** <from step 10, and how it addresses the cons above>
    ```
    Never edit or delete prior log entries — this file is append-only history.
12. **Commit** the change with `git commit` (no `git push`). Commit message format, exactly:
    ```
    Iteration #<index> - <brief description>
    ```
13. **Report back to the operator**: the hypothesis tried, what was built, the pros/cons
    found (from the log-analysis report), and how the report builder changed for next time.
14. **Watch for module-split conditions.** If the app has grown to the point that a module or
    sub-module makes sense (per `docs/tz.md` §6.14), propose/perform the split as its own
    logged iteration, set up a scoped report + hypothesis track for the new module, and
    migrate the build to Gradle multi-project if not already done.

Do not reorder, merge, or silently skip these steps. If a step doesn't apply (e.g., no
module split this iteration), say so explicitly in the log/summary rather than omitting it.

## Testing expectations

Every iteration that touches the storage service, the DNA/report class, the log-analysis
facade, or their sandboxing guarantees should keep the automated test suite green and
extend it where coverage is missing, per `docs/tz.md` §5.4:

- Security/sandboxing: path traversal, symlink escape, 500 MB boundary — verified through
  both the web/HTTP path and the MCP tools, not just one of them
- Functional: DNA parsing (line-wrapped, mixed case, ambiguous characters, empty/malformed
  input), report generation end-to-end, report regeneration clearing prior output
- Algorithms: every text-processing algorithm used for log analysis has unit tests for
  typical input, empty/malformed input, and size limits
- Log-analysis facade: contract tests lock `analyze(String)` / `analyzeToJson(String)` and
  the required JSON keys
- Don't merge a change that weakens or removes an existing test without a logged reason

Before pros/cons, run the suite so DEBUG output lands in `reports/logs/test.log`. A failing
test suite is not a valid stopping point for an iteration.

## Things you should never do in this repo

- Never commit files under `build/reports/` or `reports/logs/`.
- Never bypass the storage service for file I/O.
- Never `git push`.
- Never invent a fixed, hard-coded DNA→feature mapping table — that decision is meant to
  stay LLM-driven per iteration, per `docs/tz.md`.
- Never propose a hypothesis that's a restatement or trivial variant of one already in
  `docs/log.md` — check the log first.
- Never ship a "new" report builder version that's a cosmetic tweak of the previous one —
  it must represent a genuinely different way of reading the DNA.
- Never skip the honesty of step 6 (usefulness analysis) to make a hypothesis look better
  than it is — a weak result, reported accurately, is more valuable to the project than an
  inflated one.
- Never write Pros/Cons about log analysis without reading `reports/logs/report.json`.
- Never introduce a database, a different web stack, or a different build tool without an
  explicit update to `docs/tz.md` first.
