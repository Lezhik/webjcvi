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
consider **both** surfaces, not just the web UI — see Section 5.5 and Section 6.5.

A DNA file (`jcvi-dna.txt`, located at the project root) is treated as a **growth ruleset** —
but not an arbitrary one. The working premise of this project is that **the DNA encodes
hidden rules for a system that performs generally useful work, unrelated to the DNA itself
and unrelated to this project's own existence.** What that useful work actually is — what
kind of system, what capabilities, what purpose — is **not known in advance**. It must be
**gradually deciphered** across iterations by forming hypotheses about what the DNA encodes,
testing each hypothesis by building the functionality it implies, and evaluating whether the
result is genuinely useful (as a standalone capability, not just "useful for reading DNA" or
"useful for this project"). See Section 1.1 and Section 6 for how this decryption process
works mechanically.

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

Treat the DNA as an encrypted message whose plaintext is "a spec for some generally useful
system." Nobody knows the plaintext up front, including the operator. The iteration loop
exists to decrypt it incrementally:

- Each iteration proposes a **hypothesis**: "the hidden rule the DNA is encoding might be
  ___, and if so, the system should be able to do ___."
- The hypothesis is tested by actually **building** the functionality it implies (on the web
  UI, the MCP server, or both).
- The application **writes DEBUG logs** while its algorithms run (especially during the
  automated test suite). Those logs are then **analyzed by the log-analysis facade**
  (Section 5.6). Usefulness of the current hypothesis for **log analysis** is judged from
  that JSON report (`reports/logs/report.json`), not from speculation about what the tools
  might do.
- The resulting functionality is also judged on its own merits: **is it actually useful**,
  as a general-purpose capability, independent of the fact that it came from a DNA file or
  that this is a research project?
- Every hypothesis has pros and cons as a decryption of the DNA. **Pros and cons that
  concern log analysis must be derived from the log-analysis report** produced in
  Section 6.8, after the test journal exists. These are captured explicitly every iteration.
- The **DNA report builder itself is part of what evolves**. Each iteration produces a new
  version of the report-building logic, informed by the current hypothesis's pros and cons,
  intended to surface whatever information would help resolve the contradictions found so
  far. A report builder version must be meaningfully different from every previous version —
  not a cosmetic tweak — because it represents a genuinely different angle of attack on
  decrypting the same DNA (see Section 6.10).
- **Hypotheses must not repeat.** Before proposing a new hypothesis, the agent must check
  prior hypotheses recorded in `docs/log.md` and ensure the new one is meaningfully distinct
  — a different theory of what the DNA encodes, not a restatement or minor variant of one
  already tried.
- Consequently, the application's functionality may swing meaningfully between iterations
  as different hypotheses are tried, abandoned, refined, or combined. This is expected —
  the goal is convergence toward a hypothesis that actually explains the DNA well *and*
  produces something genuinely useful, not steady incremental feature accumulation on a
  single fixed theory.

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
  rule in Section 6.14), it should be migrated to a Gradle multi-module (multi-project)
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
    contradictions/cons identified for the current hypothesis (see Section 6.10).
  - The DNA reader/report builder must be re-invokable at will (idempotent): every
    iteration starts by regenerating the report from scratch based on current DNA + current
    codebase state.

### 5.3 Report lifecycle

- DNA reports are generated into `build/reports/` (or a module-specific subfolder once
  module splitting begins — see 6.14).
- Log-analysis artifacts live under `reports/logs/` (Section 5.6): the test DEBUG
  journal (`test.log`) and the JSON analysis (`report.json`).
- `build/reports/` and `reports/` are **excluded from Git** (must be in `.gitignore`).
  Never commit generated DNA reports, test journals, or log-analysis JSON.
- At the start of the **preparation phase** (initial setup, before the iteration loop
  begins), a "DNA decoder" step must:
  1. Delete any pre-existing reports in `build/reports/`, if present
  2. Read the DNA file
  3. Generate a fresh report (using the initial report builder — see Section 6.10 for how it
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
  - The log-analysis facade (Section 5.6) has contract tests that lock the analyze-to-JSON
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
  (re)generation, read the current report(s), and list/read files within the sandboxed
  storage (respecting the same sandboxing and 500 MB limit as 5.1 — the MCP surface must not
  become a bypass of those rules).
- Which specific tools/resources exist, and how they're named/shaped, grows iteration by
  iteration like everything else — decided by the LLM based on the report/log/spec, not
  fixed upfront.
- Automated tests (5.4) apply equally to the MCP surface: sandboxing and size-limit
  enforcement must be verified through the MCP tools too, not only through the web/HTTP path.

### 5.6 Log-analysis facade

- **One class with a fixed API** is responsible for analyzing caller-supplied log text by
  running the existing text-processing algorithms (not a parallel implementation). Input is
  a single `String` (the log body). Output is a structured JSON report. The Java method
  signatures must stay stable: `analyze(String text)` returns a structured report object,
  and `analyzeToJson(String text)` returns that report as JSON. Automated tests must fail
  if required JSON keys are removed or renamed, or if those method signatures change.
- Default relative paths (project-root sandbox, via the storage service when reading or
  writing files):
  - Test DEBUG journal: `reports/logs/test.log`
  - Log-analysis JSON: `reports/logs/report.json`
- `reports/logs/` is **excluded from Git**. Never commit generated journals or JSON
  reports.
- The facade must not read `jcvi-dna.txt` and must not touch the filesystem itself. Callers
  that persist `report.json` or read `test.log` go through `FileStorageService`.
- The test DEBUG journal itself is written by the logging framework (Logback) during
  Gradle tests, not by application code calling `java.nio.file.Files`.
- Algorithm DEBUG logs (Section 5.7) are the preferred input during an iteration: the test
  suite writes `test.log`, then the facade analyzes that journal.
- Expose the same facade on the web API and the MCP server so both surfaces can analyze
  caller-supplied log text without duplicating the analysis logic.

### 5.7 Algorithm DEBUG logging

- Every text-processing algorithm must emit **DEBUG**-level diagnostic logs describing
  inputs, counts, and outcomes that matter for log analysis.
- Every log call must be wrapped so the message is not built when DEBUG is off:

```
if (log.isDebugEnabled()) {
    log.debug(...);
}
```

- The Gradle test task used before pros/cons (Section 6.7) must enable DEBUG for
  `org.webjcvi` and persist the journal at `reports/logs/test.log`.

## 6. Iteration Algorithm

Each iteration, performed in order by the operator/agent via Cursor. This algorithm exists to
drive the decryption process from Section 1.1 — treat the hypothesis/report-builder steps as
seriously as the code-change steps; they are not bookkeeping, they're the actual research
method of this project.

### 6.1 Regenerate the DNA report
Invoke the fixed-API DNA reader/report class to rebuild the report, using **the report
builder version currently in place** (i.e., the one produced at the end of the previous
iteration's Section 6.10), based on the DNA file and the code as it exists after the previous
iteration.

### 6.2 Read the spec
Read `docs/tz.md` (this document) in full.

### 6.3 Read the decision log
Read `docs/log.md` in full, in particular every prior hypothesis and its recorded pros/cons.
This is required input for Section 6.4 — you cannot form a valid new hypothesis without
knowing what's already been tried.

### 6.4 Form a hypothesis about the hidden rules
Based on the freshly generated report and the full hypothesis history in the log, propose a
hypothesis: *what generally useful system might this DNA be encoding rules for, and what
should that system therefore be able to do?* Per Section 1.1, this hypothesis must be
**meaningfully different from every previous hypothesis** recorded in `docs/log.md` — not a
restatement, not a minor variant. If the log shows an existing hypothesis with unresolved
cons, this iteration's hypothesis may attempt to *refine or replace* it, but the replacement
itself must still be a distinct theory, not the same one restated.

### 6.5 Change the web and MCP functionality
Implement the code change(s) needed to build out (or test) what this iteration's hypothesis
implies the system should do. Explicitly consider **both surfaces**: does this hypothesis's
functionality belong on the web UI, the MCP server, or both? Add the capability to the
shared underlying class(es) first, then expose it through whichever surface(s) make sense —
don't default to web-only. The scope of a single iteration's change should be small and
reviewable (one coherent step of growth, not a rewrite).

### 6.6 Analyze the resulting code
Step back and evaluate the functionality you just built **on its own merits** — as if you
didn't know it came from a DNA file or that this is a research project. Would a person or
another piece of software genuinely want this capability? Is it coherent, or does it feel
arbitrary/forced? Record this assessment honestly, including if the answer is "not very
useful" — that's a legitimate and important outcome, not a failure to hide. This is the
code-level judgment. **Log-analysis usefulness is judged later, from the JSON report in
Section 6.9**, not guessed here.

### 6.7 Run the automated tests and keep the DEBUG journal
Run the **full** automated test suite. Tests must stay green (Section 5.4). The Gradle
test task must enable DEBUG for `org.webjcvi` and persist the journal at
`reports/logs/test.log` (Section 5.7), replacing any previous journal for this run.
Every text-processing algorithm must emit DEBUG logs during this run.

### 6.8 Analyze the test journal
After the DNA report exists (6.1) and the test journal exists (6.7), **and before
pros/cons**, call the log-analysis facade (Section 5.6) on the journal text. Persist
the JSON output to `reports/logs/report.json` through `FileStorageService` (for example
via the `analyzeLogs` Gradle task). Do not skip this step even if the journal is empty.

### 6.9 Determine the pros and cons of the current DNA decryption
**Read `reports/logs/report.json`.** Pros and cons of the hypothesis as a log-analysis
system must be formed from that report (counts, sections, truncation, what the algorithms
actually found in the app's own DEBUG journal), together with the hypothesis from 6.4 and
the code analysis from 6.6:
- **Pros:** what does this hypothesis explain well? What structure in the DNA does it
  account for? What does the log-analysis JSON show the tools actually extracted from
  the journal?
- **Cons:** what does it fail to explain? Where does the mapping from DNA to functionality
  feel forced? What does the log-analysis JSON show as weak, empty, truncated, or
  uninformative?

Do not invent log-analysis pros/cons that are not grounded in `report.json`.

### 6.10 Build a new DNA report builder
Based on the pros and cons from 6.9, design and implement a **new version of the DNA report
builder** (Section 5.2) intended to help resolve the contradictions/cons just identified —
e.g., by surfacing different structural signals from the DNA, or presenting the existing
signals in a way that makes the next hypothesis easier to form or test. This new version
must be **meaningfully different from every previous report builder version** — a different
angle on the same DNA, not a cosmetic edit. This becomes the report builder used in the
*next* iteration's Section 6.1.

### 6.11 Record the decision
Append an entry to `docs/log.md` for this iteration, using exactly this structure:

```
## Iteration #<index>
- **Hypothesis:** <the hypothesis formed in 6.4>
- **Functionality changes:** <what was built/changed in 6.5, and on which surface(s)>
- **Pros:** <pros of this hypothesis, from 6.9, grounded in reports/logs/report.json>
- **Cons:** <cons of this hypothesis, from 6.9, grounded in reports/logs/report.json>
- **Report builder changes:** <what changed in the new report builder version from 6.10,
  and how it addresses the cons above>
```

### 6.12 Commit
Commit the change to Git (**no push**). Commit message format:

```
Iteration #<index> - <brief description>
```

### 6.13 Report to the operator
Output a summary message describing what was done this iteration: the hypothesis tried, what
was built, the pros/cons found (from the log-analysis report), and how the report builder
changed for next time.

### 6.14 Module growth
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
│   └── log.md             iteration decision log (append-only)
├── reports/
│   └── logs/              test DEBUG journal + log-analysis JSON (git-ignored)
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
- **DNA-to-code mapping logic** is intentionally left undefined here — decided fresh each
  iteration via the hypothesis process in Section 6.4–6.10, not specified as a fixed
  algorithm.
- **Report content/format** is intentionally left open-ended per Section 5.2 — expected to
  change meaningfully every iteration as new report builder versions are produced.
- **MCP tool/resource shape** is intentionally left open-ended per Section 5.5, for the same
  reason — grows/changes iteration by iteration rather than being fixed upfront.
- **Specific MCP library/SDK** for the Java/Spring Boot side is not pinned — chosen and
  recorded in `docs/log.md` when the first MCP-related iteration happens.
- **No convergence criterion is defined** for when a hypothesis is "confirmed" — the process
  is exploratory; hypotheses may be abandoned, revisited in modified form, or several may
  turn out to coexist as different modules (Section 6.14) once the project splits.
- **Stopping condition** for the overall iterative growth process is not defined — the
  process continues for as long as the operator chooses to run further iterations.