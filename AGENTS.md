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
it's weak, say so and let the next hypothesis move away from it. **Do** preserve the
previous text-analysis algorithm in the facade and the previous `docs/agent/` instruction
file; the product for agents accumulates even when the DNA theory changes.

**The useful system being grown is document and log analysis for AI agents.** Usefulness is
judged by **comparing old vs new** JSON reports on the app's DEBUG logs **and** on
`docs/tz.md`. Algorithms write DEBUG journals; the test suite persists `reports/logs/test.log`;
the text-analysis facade writes four JSON files under `reports/logs/`
(`logs-before.json`, `logs-after.json`, `tz-before.json`, `tz-after.json`). **Pros/Cons in
`docs/log.md` must be derived from that before/after comparison**, after reading the
sources, both instruction files, and the hypothesis verification criterion — not from
speculation. See `docs/tz.md` §5.6–5.8 and §6.5–6.10.

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
  when the module-splitting trigger in `docs/tz.md` (Section 6.15) is actually reached, and
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
   iteration as part of the decryption process (§1.1, §6.11): expect to replace it with a
   meaningfully different version each time, not just tweak it. The *class API* around it
   should stay stable so other components can depend on it.
5. **DNA reports live in `build/reports/`** and are regenerated from scratch at the start of
   every iteration (old reports for the current scope are cleared first). **Text-analysis
   artifacts live in `reports/logs/`** (`test.log`, `logs-before.json`, `logs-after.json`,
   `tz-before.json`, `tz-after.json`, and `report.json` as a copy of `logs-after.json`).
   Both trees must stay in `.gitignore` — never commit generated reports or journals.
   **Do commit** `docs/agent/` instruction files and `docs/log.md`.
6. Keep each iteration's code change **small and reviewable** — one coherent step of growth,
   not a rewrite or a large multi-feature commit.
7. **The MCP surface must never bypass the sandboxing/size-limit rules from #1–#3.** An MCP
   tool that reads or writes files still goes through the storage service, still respects
   the project-root sandbox, and still respects the 500 MB limit — same as the web side.
8. **Text analysis goes through one facade with a fixed API** (`docs/tz.md` §5.6): input is
   text (logs or documents), output is JSON. Web, MCP, and the iteration Gradle task must
   all call that class. Do not reimplement analysis in a controller or tool handler.
   **Do not remove the previous algorithm** when adding a new JSON section.
9. **Every text-processing algorithm logs at DEBUG**, wrapped in
   `if (log.isDebugEnabled()) { log.debug(...); }`. Do not build log messages when DEBUG
   is off.
10. **Agent instructions** (`docs/agent/iteration-<n>.md`, §5.8) are written each iteration
    and never overwritten. MCP clients must read the latest file before treating analysis
    JSON as a verdict, and must apply that file's criteria to their previous step.

## The iteration algorithm — follow every step, in order, every time

When asked to run an iteration (or when picking up work in this repo), do exactly this. This
is the actual research method of the project — the hypothesis/analysis steps are not
bookkeeping, don't shortcut them. Full text is in `docs/tz.md` §6.

1. **Regenerate the DNA report** (current builder, from `jcvi-dna.txt` and previous-iteration code).
2. **Read `docs/tz.md`** in full. It is also the document corpus for later JSON reports.
3. **Read the previous hypothesis first.** Open the latest `docs/log.md` entry and read its
   hypothesis **and** Pros/Cons (that is where to go next). Then read the rest of `docs/log.md`
   for novelty. If `docs/agent/iteration-<previous>.md` exists, read it.
4. **Form a four-part hypothesis** that is meaningfully different from every prior one, and
   that answers the previous cons. All four parts are mandatory:
   DNA/text rule; usefulness for documents and logs; practical use; verification criterion
   (pass/fail on before vs after JSON).
5. **Capture old reports (before any code change).** Analyze current `test.log` (run tests
   first if it is missing) → `reports/logs/logs-before.json`. Analyze `docs/tz.md` →
   `reports/logs/tz-before.json`. Do **not** write Pros/Cons yet.
6. **Change web and MCP functionality** implied by the hypothesis. Keep the previous
   text-analysis algorithm. **Write** `docs/agent/iteration-<index>.md` (how to use the JSON
   report; this iteration's criterion; criteria to check the previous step). Do not edit
   older instruction files.
7. **Analyze the resulting code honestly** for document/log analysis by AI agents.
8. **Run the full automated test suite** so DEBUG logs land in `reports/logs/test.log`.
   A failing suite is not a valid stopping point.
9. **Capture new reports.** Analyze the new journal → `logs-after.json` (and `report.json`
   as a copy). Analyze `docs/tz.md` again → `tz-after.json`. All four JSON files must exist.
10. **Only then form Pros/Cons.** Read: `test.log`, `docs/tz.md`, the four JSON reports
    (compare old vs new on logs **and** on tz.md), old and new agent instructions with
    criteria, the four-part hypothesis. State whether the verification criterion passed.
    Ground every claim in the four JSON files.
11. **Build a new DNA report builder version**, driven by those cons, meaningfully different
    from every previous builder.
12. **Append to `docs/log.md`** using exactly this structure — do not omit any field:
    ```
    ## Iteration #<index>
    - **Hypothesis:**
      - **DNA/text rule:** <from step 4>
      - **Usefulness for documents and logs:** <from step 4>
      - **Practical use:** <from step 4>
      - **Verification criterion:** <from step 4>
    - **Functionality changes:** <from step 6, which surface(s); previous algorithm kept>
    - **Agent instructions:** docs/agent/iteration-<index>.md (previous file kept / none)
    - **Pros:** <from step 10, four JSON reports, criterion result>
    - **Cons:** <from step 10, four JSON reports, criterion result>
    - **Report builder changes:** <from step 11, and how it addresses the cons above>
    ```
    Never edit or delete prior log entries — this file is append-only history.
13. **Commit** locally (no `git push`). Include the new `docs/agent/` file. Message:
    ```
    Iteration #<index> - <brief description>
    ```
14. **Report back to the operator**: four-part hypothesis, what was built, before/after
    comparison, criterion pass/fail, Pros/Cons, instruction path, report-builder change.
15. **Watch for module-split conditions** (`docs/tz.md` §6.15). If a split is due, treat it
    as its own logged iteration.

Do not reorder, merge, or silently skip these steps. If a step doesn't apply (e.g. no
module split, no previous agent file yet), say so explicitly in the log/summary rather
than omitting it.

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
- Text-analysis facade: contract tests lock `analyze(String)` / `analyzeToJson(String)` and
  the required JSON keys
- Don't merge a change that weakens or removes an existing test without a logged reason
- Don't remove a previous text-analysis algorithm from the facade without a logged reason

Before Pros/Cons, the four JSON reports in `reports/logs/` must exist (old and new, logs and
`docs/tz.md`). A failing test suite is not a valid stopping point for an iteration.

## Things you should never do in this repo

- Never commit files under `build/reports/` or `reports/logs/`.
- Never bypass the storage service for file I/O.
- Never `git push`.
- Never invent a fixed, hard-coded DNA→feature mapping table — that decision is meant to
  stay LLM-driven per iteration, per `docs/tz.md`.
- Never propose a hypothesis that's a restatement or trivial variant of one already in
  `docs/log.md` — check the previous entry's Pros/Cons first, then the full log.
- Never form a hypothesis that omits any of the four required parts (DNA/text rule,
  usefulness for documents and logs, practical use, verification criterion).
- Never write Pros/Cons without all four JSON reports, without comparing old vs new, or
  without reading old and new agent instructions and the verification criterion.
- Never overwrite or delete a previous `docs/agent/iteration-*.md` or remove the previous
  text-analysis algorithm from the facade.
- Never ship a "new" report builder version that's a cosmetic tweak of the previous one —
  it must represent a genuinely different way of reading the DNA.
- Never skip the honesty of step 7 (usefulness analysis) to make a hypothesis look better
  than it is — a weak result, reported accurately, is more valuable to the project than an
  inflated one.
- Never introduce a database, a different web stack, or a different build tool without an
  explicit update to `docs/tz.md` first.
