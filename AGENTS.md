# AGENTS.md — WebJCVI

This file tells any AI coding agent (Cursor, Claude Code, etc.) working in this repository
how to operate. Read this file first, before touching any code. The full specification is
in `docs/tz.md` — this file is the operating procedure, `docs/tz.md` is the source of truth
for requirements if the two ever conflict.

## What this project is

WebJCVI grows an application, iteration by iteration, out of a real DNA sequence
(`jcvi-dna.txt`). At each iteration, you (the agent) regenerate a report from the DNA and
the current code, read the spec and the decision log, and decide the next small code change.
The DNA is a **growth rule**, not a literal spec — how it maps to a code change is your
judgment call, made fresh each iteration based on what you and prior iterations have learned.

**The app has two surfaces: a web application, and an MCP server.** It is not "a web app
with MCP bolted on" — treat both as first-class. Every capability you add should live in a
shared underlying class and be exposed through whichever surface(s) make sense; growth
decisions each iteration should explicitly consider both, not default to web-only. See
`docs/tz.md` §5.5 and §6.4.

## Tech stack — do not deviate

- **Language / framework:** Java, Spring Boot, Spring WebFlux (reactive). Do not introduce
  Spring MVC (servlet stack), blocking I/O in reactive chains, or a different web framework.
- **Templating:** JTE for any rendered HTML/report views. Do not introduce Thymeleaf,
  Freemarker, JSP, or a frontend framework unless the spec is explicitly amended.
- **MCP server:** the app also exposes an MCP server surface (`docs/tz.md` §5.5). Build MCP
  tools/resources on top of the same shared classes as the web UI — never duplicate logic
  between a web controller and an MCP tool handler. If you pick an MCP library/SDK, record
  the choice in `docs/log.md`.
- **Database:** none. This project is file-based only. Do not add a DB, embedded or
  otherwise, without an explicit spec change.
- **Build tool:** Gradle with the Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`).
  Do not add a `pom.xml` or switch to Groovy DSL.
- **Module layout:** single module today. Only split into a Gradle multi-project layout
  when the module-splitting trigger in `docs/tz.md` (Section 6.8) is actually reached, and
  record that decision in the log when you do it.

## Hard architectural rules

1. **All filesystem access goes through one class with a fixed API** (the file storage
   service described in `docs/tz.md` §5.1). No other class reads or writes files directly.
   If you're about to write `Files.write(...)` or `new FileInputStream(...)` outside that
   class, stop — route it through the storage service instead.
2. **The app may only read/write inside its own project directory.** Reject `..`, absolute
   paths, and symlink escapes. This must be enforced in code, not just assumed — and it must
   have a test.
3. **500 MB max file size**, enforced on both read and write, in the storage service.
4. **DNA reading and report building goes through one class with a fixed API** (`docs/tz.md`
   §5.2). The *content* of the report is yours to decide/evolve each iteration; the *class
   API* around it should stay stable so other components can depend on it.
5. **Reports live in `build/reports/`** and are regenerated from scratch at the start of
   every iteration (old reports for the current scope are cleared first). `build/reports/`
   must stay in `.gitignore` — never commit generated reports.
6. Keep each iteration's code change **small and reviewable** — one coherent step of growth,
   not a rewrite or a large multi-feature commit.
7. **The MCP surface must never bypass the sandboxing/size-limit rules from #1–#3.** An MCP
   tool that reads or writes files still goes through the storage service, still respects
   the project-root sandbox, and still respects the 500 MB limit — same as the web side.

## The iteration algorithm — follow every step, in order, every time

When asked to run an iteration (or when picking up work in this repo), do exactly this:

1. **Regenerate the DNA report.** Call the fixed-API DNA/report class to rebuild the report
   for the current scope (whole app, or the relevant module/sub-module) from `jcvi-dna.txt`
   and the code as it stands after the previous iteration. Don't skip this even if you think
   the DNA hasn't changed — the *code* has, and the report should reflect that context too.
2. **Read `docs/tz.md`** in full (or re-confirm you have it fresh in context).
3. **Read `docs/log.md`** in full to see what's been decided and why, so you don't repeat or
   contradict prior reasoning.
4. **Decide the next code change**, using the report + log + spec together. Treat the DNA
   content as a growth constraint/inspiration for the patch, not a literal instruction set.
   Explicitly consider whether this iteration's growth belongs on the web UI, the MCP
   server, or both — don't default to web-only. Write the code change.
5. **Append to `docs/log.md`**: the iteration number, what you decided and why (tie it back
   to the report/DNA where relevant), and what changed in the code. Never edit or delete
   prior log entries — this file is append-only history.
6. **Commit** the change with `git commit` (no `git push`). Commit message format, exactly:
   ```
   Iteration #<index> - <brief description>
   ```
7. **Report back to the operator**: summarize what changed, why, and anything notable the
   report surfaced.
8. **Watch for module-split conditions.** If the app has grown to the point that a module or
   sub-module makes sense (per `docs/tz.md` §6.8), propose/perform the split as its own
   logged iteration, set up a scoped report + patch history for the new module, and migrate
   the build to Gradle multi-project if not already done.

Do not reorder, merge, or silently skip these steps. If a step doesn't apply (e.g., no
module split this iteration), say so explicitly in the log/summary rather than omitting it.

## Testing expectations

Every iteration that touches the storage service, the DNA/report class, or their sandboxing
guarantees should keep the automated test suite green and extend it where coverage is
missing, per `docs/tz.md` §5.4:

- Security/sandboxing: path traversal, symlink escape, 500 MB boundary — verified through
  both the web/HTTP path and the MCP tools, not just one of them
- Functional: DNA parsing (line-wrapped, mixed case, ambiguous characters, empty/malformed
  input), report generation end-to-end, report regeneration clearing prior output
- Don't merge a change that weakens or removes an existing test without a logged reason

Run the test suite before committing. A failing test suite is not a valid stopping point for
an iteration.

## Things you should never do in this repo

- Never commit files under `build/**`.
- Never bypass the storage service for file I/O.
- Never `git push`.
- Never invent a fixed, hard-coded DNA→feature mapping table — that decision is meant to
  stay LLM-driven per iteration, per `docs/tz.md`.
- Never introduce a database, a different web stack, or a different build tool without an
  explicit update to `docs/tz.md` first.