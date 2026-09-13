# WebJCVI — Technical Specification

## 1. Project Summary

**WebJCVI** is a research/experimental project exploring whether an application can be
"grown" iteration by iteration out of a real DNA sequence, using AI agents (LLMs) as the
decision-making and code-generation engine.

The application has **two surfaces, not one**: a web application (WebFlux/JTE) for human
viewing and interaction, and an **MCP (Model Context Protocol) server** exposing the same
capabilities (DNA reading, report generation, storage access, and whatever else grows out
of later iterations) as MCP tools/resources that other AI agents can call directly. The
project is dual-purpose by design: it's both something a person can open in a browser and
something an AI agent can attach to as an MCP client. Growth decisions each iteration should
consider **both** surfaces, not just the web UI — see Section 5.5 and Section 6.6.

A DNA file (`jcvi-dna.txt`, located at the project root) is treated as a **growth ruleset** —
but not an arbitrary one. The working premise of this project is that **the DNA encodes
hidden rules for a system that performs generally useful work, unrelated to the DNA itself
and unrelated to this project's own existence.** What that useful work actually is — what
kind of system, what capabilities, what purpose — is **not known in advance**. It must be
**gradually deciphered** across iterations by forming hypotheses about what the DNA encodes,
testing each hypothesis by building the functionality it implies, and evaluating whether the
result is genuinely useful as **document and log analysis for AI agents** (not just "useful
for reading DNA" or "useful for this project"). See Section 1.1 and Section 6 for how this
decryption process works mechanically.

At each iteration, the LLM re-reads a freshly generated report derived from the DNA and the
current codebase, consults the spec (this document) and the decision log, and decides how to
patch/extend the application — on the web side, the MCP side, or both — in service of testing
its current hypothesis about the hidden rules. Over time, as the app grows, it may split into
modules and sub-modules, each with its own report and its own patch history.

This is not a simulation of biology in the strict sense — the DNA sequence is a structured,
deterministic input that is *reinterpreted freely by the LLM* at every step as inspiration/
constraint for the next code change. The exact mapping from DNA content to code decisions is
intentionally left to the LLM's judgment at each iteration, not hard-coded.

### 1.1 The decryption model

Treat the DNA as an encrypted message whose plaintext is "a spec for a generally useful
system." The useful system this project is aiming at is **document and log analysis for AI
agents** (MCP clients and the coding agent in this repo): structured JSON reports over
caller-supplied text, plus committed instructions that tell an agent how to use those
reports and how to check its previous step. Nobody knows the DNA plaintext up front,
including the operator. The iteration loop exists to decrypt it incrementally:

- Each iteration proposes a **four-part hypothesis** (Section 6.4): the DNA/text rule; why
  that rule helps analyze **documents and logs**; how an agent or operator would use it in
  practice; and a **verification criterion** that can pass or fail against the before/after
  JSON reports.
- **Before forming a new hypothesis**, the agent **must** read the immediately previous
  hypothesis in `docs/log.md` and that entry's Pros and Cons, then the rest of the log.
  The new hypothesis must be a response to those cons (refine or replace), not a leap that
  ignores them. It must still be meaningfully distinct from every prior hypothesis.
- The hypothesis is tested by **building** the functionality it implies (web, MCP, or both)
  **and** by writing a new **agent-instruction** file (Section 5.8) for how to use the
  text-analysis report. Previous algorithms and previous instruction files are **kept**.
- Usefulness is judged by **comparing old analysis with new analysis**, never from a single
  snapshot and never from speculation. Before Pros/Cons the agent must produce **four**
  JSON reports through the text-analysis facade (Section 5.6): logs-before, logs-after,
  tz-before, tz-after (Section 6.5 and 6.9).
- Pros/Cons are written only after the agent has read: the app's DEBUG logs, `docs/tz.md`,
  the four JSON reports, the previous and new agent instructions (with criteria), and the
  four-part hypothesis including its verification criterion (Section 6.10).
- The resulting functionality is also judged on its own merits: would an AI agent analyzing
  documents or logs actually want this, independent of DNA or this research project?
- The **DNA report builder itself is part of what evolves**. Each iteration produces a new
  version of the report-building logic, informed by the current hypothesis's pros and cons
  (see Section 6.11).
- Consequently, the application's functionality may swing between iterations as hypotheses
  are tried, abandoned, or refined. Previous text-analysis algorithms stay in the facade
  (additive JSON sections); the product for agents accumulates, even when the DNA theory
  changes.

## 2. Operating Model

- Each iteration is **manually triggered by the operator via Cursor** (or an equivalent
  AI-assisted IDE). There is no autonomous scheduler and no self-triggering loop inside the
  application itself — the app only exposes the building blocks (DNA reader, report
  generator, file manager) that the human-operated AI agent calls into during an iteration.
- The operator/agent is expected to follow the iteration algorithm in Section 6 in order,
  every time, without skipping steps.

## 3. Tech Stack

- **Language / Framework:** Java, Spring Boot, Spring WebFlux (reactive)
- **Templating:** JTE (Java Template Engine) for any HTML views/reports rendered in the web UI
- **MCP server:** the application also exposes an MCP server interface so external AI agents
  can call its capabilities directly as MCP tools/resources (not just view them via the web
  UI). Prefer a Java MCP SDK/library that integrates cleanly with Spring Boot/WebFlux over
  hand-rolling the protocol; the exact library choice is left to the implementing iteration,
  but must be recorded in `docs/log.md` when chosen.
- **Database:** none — the application is file-based only
- **Build tool:** Gradle with the Kotlin DSL (`build.gradle.kts`), single module for now,
  with the codebase organized into growing sub-packages (`dna`, `report`, `storage`, `web`,
  etc.). If/when the project genuinely outgrows a single module (per the module-splitting
  rule in Section 6.15), it should be migrated to a Gradle multi-module (multi-project)
  layout at that point — this is not required from day one.
- **Version control:** Git, local commits only (no `push` as part of the automated flow)

## 4. Data Input

### 4.1 DNA file

- Location: project root, filename `jcvi-dna.txt`
- Format: **FASTA-like without header** — the file contains only nucleotide characters
  (`A`, `T`, `G`, `C`, possibly `N` or lowercase variants) wrapped across multiple lines
  (typical FASTA line wrapping, e.g. 60/70/80 chars per line), but **without** a leading
  `>` header line.
- The DNA reader must be tolerant of:
  - Trailing/leading whitespace and blank lines
  - Line-wrapped sequences of arbitrary width
  - Mixed case nucleotide letters (normalize to uppercase)
  - Unexpected characters (`N`, IUPAC ambiguity codes) — these must not crash parsing;
    document how they are handled (e.g., counted separately, flagged in the report)

## 5. Core Application Requirements

### 5.1 File access sandboxing

- The application may read and write **local files only within its own project directory**
  (no access outside the project root — must be enforced, not just assumed).
- Maximum file size for any single file the app reads or writes: **500 MB**. Attempts to
  exceed this must fail gracefully with a clear error, not crash the process or silently
  truncate.
- **All file I/O must go through a single class with a fixed, stable API** (e.g.
  `FileStorageService` or similar). No other class/component is allowed to touch the
  filesystem directly. This class is responsible for:
  - Path resolution and sandboxing (rejecting any path that escapes the project root,
    including via `..`, symlinks, or absolute paths)
  - Enforcing the 500 MB size limit on read and write
  - Basic read/write/delete/list operations used by the rest of the app

### 5.2 DNA reading & report building

- **A single class with a fixed API** is responsible for:
  - Reading and parsing `jcvi-dna.txt` (via the file storage class from 5.1 — never
    reading the file directly)
  - Building a report from the DNA content
  - The exact *content* of the report (what statistics, patterns, or summaries it contains)
    is **decided by the LLM at each iteration** as part of the decryption process described
    in Section 1.1 — the report is not a fixed, hard-coded format, and its content is
    expected to change meaningfully from iteration to iteration as new hypotheses about the
    hidden rules are tried and the pros/cons of the current one are identified. The class's
    fixed API should therefore be generic enough to support an evolving report structure
    (e.g., returning a structured/extensible report object or writing a
    markdown/JSON report file whose section content varies over time).
  - Each new version of the report builder must be **meaningfully different from previous
    versions**, not a cosmetic adjustment — it should represent a genuinely different
    approach to extracting signal from the DNA, chosen to resolve the specific
    contradictions/cons identified for the current hypothesis (see Section 6.11).
  - The DNA reader/report builder must be re-invokable at will (idempotent): every
    iteration starts by regenerating the report from scratch based on current DNA + current
    codebase state.

### 5.3 Report lifecycle

- DNA reports are generated into `build/reports/` (or a module-specific subfolder once
  module splitting begins — see 6.15).
- Text-analysis artifacts live under `reports/logs/` (Section 5.6): the test DEBUG
  journal (`test.log`) and the four JSON reports (`logs-before.json`, `logs-after.json`,
  `tz-before.json`, `tz-after.json`). `report.json` is a copy of `logs-after.json` for
  the existing Gradle task.
- Agent-instruction files live under `docs/agent/` (Section 5.8) and **are committed**.
- `build/reports/` and `reports/` are **excluded from Git** (must be in `.gitignore`).
  Never commit generated DNA reports, test journals, or analysis JSON. Do commit
  `docs/agent/` and `docs/log.md`.
- At the start of the **preparation phase** (initial setup, before the iteration loop
  begins), a "DNA decoder" step must:
  1. Delete any pre-existing reports in `build/reports/`, if present
  2. Read the DNA file
  3. Generate a fresh report (using the initial report builder — see Section 6.11 for how it
     evolves from there)
- The same regeneration must happen as **step 1 of every iteration** (Section 6.1) — the
  report always reflects the current state of the DNA and the code generated in the
  previous iteration, using whatever report builder version is currently in place.

### 5.4 Automated tests

The project must include automated tests covering at minimum:

- **Security / sandboxing tests:**
  - Verify the file storage class rejects path traversal attempts (`../`, absolute paths,
    symlink escapes) outside the project root
  - Verify the 500 MB size limit is enforced on both read and write paths
  - Verify no other component bypasses the file storage class (can be enforced via
    architecture tests, e.g. ArchUnit, in addition to unit tests)
- **Basic functionality tests:**
  - DNA parsing correctness (valid FASTA-without-header input, line-wrapped, mixed case,
    ambiguous/invalid characters)
  - Report generation runs end-to-end without error and produces output in
    `build/reports/`
  - Report regeneration correctly clears/replaces prior reports
  - Every text-processing algorithm used for log analysis (tape, banner split, pair-drift,
    unwrap, rare islands, tokens, k-mer stamps, palindromes, stem loops, fuzzy find) has
    unit tests covering its functional behavior (typical input, empty/malformed input,
    size limit)
  - The text-analysis facade (Section 5.6) has contract tests that lock the analyze-to-JSON
    API (required keys, types, `apiVersion`)
- **Limits/edge cases:**
  - Empty DNA file
  - Malformed DNA file (non-nucleotide content)
  - File exactly at / just above the 500 MB boundary

### 5.5 MCP server surface

- The application exposes its capabilities via an MCP server in addition to the web UI.
  This is a first-class surface, not an afterthought bolted on at the end.
- MCP tools/resources should be built **on top of the same underlying classes** as the web
  UI (the file storage service from 5.1, the DNA/report class from 5.2, and whatever
  domain classes grow out of later iterations) — never duplicate logic between the web
  controllers and the MCP tool handlers. If a capability is useful to expose, expose it once
  and let both surfaces call it.
- At minimum, plan for MCP tools that let an external agent: trigger DNA report
  (re)generation, read the current report(s), analyze caller-supplied logs or documents
  through the text-analysis facade (Section 5.6), and list/read files within the sandboxed
  storage (respecting the same sandboxing and 500 MB limit as 5.1 — the MCP surface must not
  become a bypass of those rules). An MCP client that interprets analysis JSON **must**
  first read the current agent-instruction file under `docs/agent/` (via `read_file` or an
  equivalent) and apply that file's verification criteria to its previous step before
  treating the JSON as a verdict.
- Which specific tools/resources exist, and how they're named/shaped, grows iteration by
  iteration like everything else — decided by the LLM based on the report/log/spec, not
  fixed upfront.
- Automated tests (5.4) apply equally to the MCP surface: sandboxing and size-limit
  enforcement must be verified through the MCP tools too, not only through the web/HTTP path.

### 5.6 Text-analysis facade

- **One class with a fixed API** analyzes caller-supplied **text** (application logs **or**
  documents such as `docs/tz.md`) by running the existing text-processing algorithms (not a
  parallel implementation). Input is a single `String`. Output is a structured JSON report.
  The Java method signatures must stay stable: `analyze(String text)` returns a structured
  report object, and `analyzeToJson(String text)` returns that report as JSON. Automated
  tests must fail if required JSON keys are removed or renamed, or if those method
  signatures change.
- **Previous algorithms stay.** Each iteration may add a JSON section and a shared
  algorithm class. It must not remove, rename away, or replace the previous iteration's
  algorithm. The facade is an accumulating toolkit for document/log analysis, not a
  single-theory scanner that gets rewritten.
- Default relative paths (project-root sandbox, via the storage service when reading or
  writing files):
  - Test DEBUG journal: `reports/logs/test.log`
  - Logs, before this iteration's algorithm: `reports/logs/logs-before.json`
  - Logs, after this iteration's algorithm and new tests: `reports/logs/logs-after.json`
  - `docs/tz.md`, before this iteration's algorithm: `reports/logs/tz-before.json`
  - `docs/tz.md`, after this iteration's algorithm: `reports/logs/tz-after.json`
  - Compatibility copy of the after-logs report: `reports/logs/report.json`
- `reports/logs/` is **excluded from Git**. Never commit generated journals or JSON
  reports.
- The facade must not read `jcvi-dna.txt` and must not touch the filesystem itself. Callers
  that persist JSON or read `test.log` / `docs/tz.md` go through `FileStorageService`.
- The test DEBUG journal itself is written by the logging framework (Logback) during
  Gradle tests, not by application code calling `java.nio.file.Files`.
- Expose the same facade on the web API and the MCP server so both surfaces can analyze
  caller-supplied logs or documents without duplicating the analysis logic.

### 5.7 Algorithm DEBUG logging

- Every text-processing algorithm must emit **DEBUG**-level diagnostic logs describing
  inputs, counts, and outcomes that matter for log and document analysis.
- Every log call must be wrapped so the message is not built when DEBUG is off:

```
if (log.isDebugEnabled()) {
    log.debug(...);
}
```

- The Gradle test task used for the before-snapshot (Section 6.5) and the after-journal
  (Section 6.8) must enable DEBUG for `org.webjcvi` and persist the journal at
  `reports/logs/test.log`.

### 5.8 Agent instructions for text-analysis reports

- Each iteration writes a **new** instruction file for AI agents:
  `docs/agent/iteration-<index>.md` (English). These files **are committed**.
- **Never edit or delete** a previous `docs/agent/iteration-*.md`. The previous algorithm
  and the previous instructions must remain so an agent can compare old vs new.
- The new file must tell an MCP client or coding agent how to use the **text-analysis JSON**
  (logs and documents), not how to read DNA. Required sections:
  1. **How to use this report** — which JSON keys matter, what they mean for documents vs
     logs, which MCP tools to call next.
  2. **Verification criterion (this iteration)** — the pass/fail test from the four-part
     hypothesis (Section 6.4). An agent must apply this to the before/after JSON pair
     before treating the analysis as a verdict.
  3. **Check the previous step** — explicit criteria for verifying the *previous*
     iteration's work: quote or cite that iteration's criterion and Pros/Cons, and state
     what the agent must re-check (for example: did the previous `cheaperEnd` claim still
     hold on `logs-before.json`?). If no previous instruction file exists, say so and skip
     the cite, but still describe how to treat a missing baseline.
- MCP clients should `read_file` the latest `docs/agent/iteration-*.md` **before**
  interpreting `analyze_logs` (or equivalent) output.

## 6. Iteration Algorithm

Each iteration, performed in order by the operator/agent via Cursor. This algorithm exists to
drive the decryption process from Section 1.1 — treat the hypothesis, four-report comparison,
and agent-instruction steps as seriously as the code-change steps; they are not bookkeeping,
they are the actual research method of this project.

Do not reorder, merge, or silently skip these steps. If a step does not apply (e.g. no
previous agent-instruction file yet, no module split), say so explicitly in the log/summary
rather than omitting it.

### 6.1 Regenerate the DNA report
Invoke the fixed-API DNA reader/report class to rebuild the report, using **the report
builder version currently in place** (i.e., the one produced at the end of the previous
iteration's Section 6.11), based on the DNA file and the code as it exists after the previous
iteration.

### 6.2 Read the spec
Read `docs/tz.md` (this document) in full. This file is also the **document corpus** that
will be analyzed in 6.5 and 6.9.

### 6.3 Read the decision log (previous hypothesis first)
**Required order, do not skip:**
1. Read the **immediately previous** `docs/log.md` entry: its four-part hypothesis (or
   legacy Hypothesis field) **and** its Pros and Cons. This is the steering signal for
   where to go next — the new hypothesis must answer those cons.
2. Then read `docs/log.md` **in full** so the new hypothesis is not a restatement of any
   earlier one.
3. If `docs/agent/iteration-<previous>.md` exists, read it. That file's verification
   criterion is what 6.6's new instructions must tell an agent to re-check.

You cannot form a valid new hypothesis without this step.

### 6.4 Form a four-part hypothesis
Based on the freshly generated DNA report (6.1) and the previous hypothesis + Pros/Cons
(6.3), propose a hypothesis that is **meaningfully different from every previous one** in
`docs/log.md`. Refining a prior hypothesis is fine; restating it is not. If the previous
entry has unresolved cons, this iteration's hypothesis should refine or replace that
theory, but the replacement must still be a distinct theory.

The hypothesis **must** contain these four parts, written explicitly (not implied):

1. **DNA/text rule** — what structural rule the DNA (and therefore arbitrary text) might
   be encoding, pointed at a concrete signal from the DNA report in 6.1 (not a vague
   after-the-fact rationalization).
2. **Usefulness for documents and logs** — why that rule would help an AI agent analyze
   documents (starting with `docs/tz.md`) and application logs, not only DNA.
3. **Practical use** — how an agent or operator would use the capability (which MCP tools,
   which JSON keys, what decision it supports: cluster, unwrap, pick an identifier end,
   etc.).
4. **Verification criterion** — a pass/fail test over the **before vs after** JSON pair
   (logs and tz.md). Example shape: "on `tz-after.json` section X, metric Y is Z, **and**
   it differs from `tz-before.json` in this way; on logs, …". Pros/Cons later must say
   whether this criterion passed.

The useful system under test is document/log analysis for AI agents, not "viewing DNA."

### 6.5 Capture the before analyses (old reports)
**Before changing code**, produce the two *before* JSON reports through the text-analysis
facade **as it exists after the previous iteration** (no new algorithm yet):

1. If `reports/logs/test.log` is missing, run the full automated test suite first so the
   journal exists (Section 5.7). If it already exists from the previous iteration, use it.
2. Read `test.log` via `FileStorageService`, call `analyze`/`analyzeToJson`, persist
   `reports/logs/logs-before.json`.
3. Read `docs/tz.md` via `FileStorageService`, call the same facade, persist
   `reports/logs/tz-before.json`.

Do not skip this even if the journal looks empty. These two files are the **old analysis**.
The agent must not form Pros/Cons yet.

### 6.6 Change functionality and write agent instructions
Implement the code change(s) implied by the four-part hypothesis. Explicitly consider
**both surfaces**: web UI, MCP, or both — don't default to web-only. Shared class first,
then surfaces. Keep the change small and reviewable.

**Keep the previous text-analysis algorithm** in the facade (Section 5.6). Add a new
section/class if needed; do not delete the previous one.

**Write** `docs/agent/iteration-<index>.md` (Section 5.8). Do not modify previous
instruction files. The new file must include the verification criterion from 6.4 and
criteria for checking the previous step (from 6.3).

### 6.7 Analyze the resulting code
Evaluate the functionality **on its own merits** for document/log analysis by AI agents.
Would an agent actually want this? Is it coherent, or forced? Record this honestly,
including "not very useful." This is the code-level judgment. Usefulness against the
verification criterion is judged later, from the four JSON reports in Section 6.10,
not guessed here.

### 6.8 Run the automated tests and keep the DEBUG journal
Run the **full** automated test suite **after** the code change. Tests must stay green
(Section 5.4). Persist `reports/logs/test.log`, replacing the previous journal for this
run. Every text-processing algorithm must emit DEBUG logs during this run. A failing
suite is not a valid stopping point.

### 6.9 Capture the after analyses (new reports)
After the new journal exists (6.8), **and still before Pros/Cons**, call the facade
**as it exists after this iteration's algorithm**:

1. Analyze the new `test.log` → `reports/logs/logs-after.json`. Also write
   `reports/logs/report.json` as a copy of `logs-after.json` (Gradle `analyzeLogs` may
   produce this copy).
2. Analyze `docs/tz.md` again (same document bytes unless this iteration edited the spec)
   → `reports/logs/tz-after.json`.

All **four** files must now exist: `logs-before.json`, `logs-after.json`,
`tz-before.json`, `tz-after.json`. Do not skip even if a journal or document looks empty.

### 6.10 Determine Pros and Cons (only after the comparison packet)
**Forbidden:** writing Pros/Cons from a single `report.json`, from speculation, or before
the four reports exist.

The agent **must** read, in this spirit (all of them, not a subset):

- The DEBUG logs (`reports/logs/test.log`) and `docs/tz.md` as source text
- The four JSON reports, **comparing old vs new** for logs and for `docs/tz.md`
- Previous agent instructions (if any) and the new `docs/agent/iteration-<index>.md`,
  including both files' verification criteria and the "check the previous step" section
- The four-part hypothesis from 6.4, especially the verification criterion
- The code analysis from 6.7

Then form Pros and Cons:

- **Pros:** what the DNA/text rule explains; what the **delta** (after − before) on logs
  and on `docs/tz.md` actually shows; whether the verification criterion **passed**;
  whether the new instructions would let an MCP agent use that delta.
- **Cons:** where the mapping is forced; where after ≈ before (no new signal); where the
  criterion **failed**; where logs and `docs/tz.md` disagree; where the new JSON section
  restates an old one.

Ground every log/document claim in the four JSON files (quote keys and values). State
explicitly whether the criterion passed.

### 6.11 Build a new DNA report builder
Based on the pros and cons from 6.10, design and implement a **new version of the DNA report
builder** (Section 5.2) intended to help resolve the contradictions/cons just identified.
This new version must be **meaningfully different from every previous report builder
version**. This becomes the report builder used in the *next* iteration's Section 6.1.

### 6.12 Record the decision
Append an entry to `docs/log.md` for this iteration, using exactly this structure:

```
## Iteration #<index>
- **Hypothesis:**
  - **DNA/text rule:** <6.4 part 1>
  - **Usefulness for documents and logs:** <6.4 part 2>
  - **Practical use:** <6.4 part 3>
  - **Verification criterion:** <6.4 part 4>
- **Functionality changes:** <what was built in 6.6, which surface(s); previous algorithm kept>
- **Agent instructions:** `docs/agent/iteration-<index>.md` (previous file kept / none)
- **Pros:** <from 6.10, grounded in the four JSON reports and the criterion result>
- **Cons:** <from 6.10, grounded in the four JSON reports and the criterion result>
- **Report builder changes:** <what changed in 6.11, and how it addresses the cons above>
```

Never edit or delete prior log entries — this file is append-only history.

### 6.13 Commit
Commit the change to Git (**no push**). Include `docs/agent/iteration-<index>.md` and
`docs/log.md`. Do not commit `reports/logs/` or `build/reports/`. Commit message format:

```
Iteration #<index> - <brief description>
```

### 6.14 Report to the operator
Output a summary: the four-part hypothesis, what was built, the before/after comparison
(logs and tz.md), whether the verification criterion passed, the Pros/Cons, the agent-
instruction path, and how the DNA report builder changed.

### 6.15 Module growth
As the application grows, it may be split into modules and sub-modules. When this happens:
- Each module/sub-module gets **its own DNA-derived report**, **its own hypothesis
  track**, and **its own patch/iteration history** going forward, scoped to that module
- The decision to split, and the resulting structure, should itself be recorded in
  `docs/log.md` as an iteration
- The single-module Gradle layout (Section 3) should transition to a Gradle multi-project
  layout at this point, if not done already

## 7. Directory Layout (initial)

```
/                          project root
├── jcvi-dna.txt           DNA input file (read-only from the app's perspective)
├── docs/
│   ├── tz.md              this specification
│   ├── log.md             iteration decision log (append-only)
│   └── agent/             agent instructions per iteration (committed, never overwritten)
│       └── iteration-<n>.md
├── reports/
│   └── logs/              test DEBUG journal + four JSON reports (git-ignored)
├── build/
│   └── reports/           generated DNA reports (git-ignored)
├── src/
│   └── main/java/...      application code (dna / report / storage / web / mcp / logs)
├── .gitignore             must exclude build/reports/ and reports/
├── build.gradle.kts
└── settings.gradle.kts
```

## 8. Open Points / Assumptions Made

- **What the DNA actually encodes** is unknown by design (Section 1.1) — this is not a gap
  to fill in this document, it's the thing the iteration loop exists to discover.
- **Useful-system target** for this phase of the project is document and log analysis for
  AI agents. The DNA remains the ruleset; the product being grown is the accumulating
  text-analysis facade plus agent instructions, not a DNA viewer.
- **Old vs new reports:** `*-before.json` is the facade **before** this iteration's new
  algorithm (and, for logs, `test.log` as of that moment). `*-after.json` is the facade
  **after** the new algorithm (and, for logs, the new test journal). `docs/tz.md` is the
  same document both times unless this iteration edited the spec, so the tz pair isolates
  the algorithm delta.
- **First iteration after this spec:** if no `docs/agent/iteration-*.md` exists yet, record
  "none" for previous instructions and still write the first file. If `test.log` is
  missing in 6.5, run tests once to create it.
- **Gradle `analyzeLogs`** may still write `report.json`; the iteration agent is responsible
  for also persisting the four named JSON files through `FileStorageService`. Task wiring
  can catch up in a later iteration; skipping the four files is not allowed.
- **DNA-to-code mapping logic** is intentionally left undefined here — decided fresh each
  iteration via the hypothesis process in Section 6.4–6.11, not specified as a fixed
  algorithm.
- **Report content/format** is intentionally left open-ended per Section 5.2 — expected to
  change meaningfully every iteration as new report builder versions are produced.
- **MCP tool/resource shape** is intentionally left open-ended per Section 5.5, for the same
  reason — grows/changes iteration by iteration rather than being fixed upfront.
- **Specific MCP library/SDK** for the Java/Spring Boot side is not pinned — chosen and
  recorded in `docs/log.md` when the first MCP-related iteration happens.
- **No convergence criterion is defined** for when a hypothesis is "confirmed" — the
  process is exploratory; hypotheses may be abandoned, revisited in modified form, or
  several may turn out to coexist as different modules (Section 6.15) once the project
  splits. The per-iteration **verification criterion** (Section 6.4) is not project-wide
  confirmation; it only scores that iteration's before/after reports.
- **Stopping condition** for the overall iterative growth process is not defined — the
  process continues for as long as the operator chooses to run further iterations.
