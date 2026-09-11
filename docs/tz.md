# WebJCVI — Technical Specification (TZ)

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
consider **both** surfaces, not just the web UI — see Section 5.5 and Section 6.4.

A DNA file (`jcvi-dna.txt`, located at the project root) is treated as a **growth ruleset**:
at each iteration, the LLM re-reads a freshly generated report derived from the DNA and the
current codebase, consults the spec (this document) and the decision log, and decides how to
patch/extend the application — on the web side, the MCP side, or both. Over time, as the app
grows, it may split into modules and sub-modules, each with its own report and its own patch
history.

This is not a simulation of biology in the strict sense — the DNA sequence is a structured,
deterministic input that is *reinterpreted freely by the LLM* at every step as inspiration/
constraint for the next code change. The exact mapping from DNA content to code decisions is
intentionally left to the LLM's judgment at each iteration, not hard-coded.

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
  rule in Section 6.8), it should be migrated to a Gradle multi-module (multi-project)
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
    is **decided by the LLM at each iteration**, based on what was useful/learned from
    previous iterations — the report is not a fixed, hard-coded format. The class's fixed
    API should therefore be generic enough to support an evolving report structure
    (e.g., returning a structured/extensible report object or writing a
    markdown/JSON report file whose section content varies over time).
  - The DNA reader/report builder must be re-invokable at will (idempotent): every
    iteration starts by regenerating the report from scratch based on current DNA + current
    codebase state.

### 5.3 Report lifecycle

- Reports are generated into `build/reports/` (or a module-specific subfolder once module
  splitting begins — see 6.8).
- `build/reports/` is **excluded from Git** (must be in `.gitignore`).
- At the start of the **preparation phase** (initial setup, before the iteration loop
  begins), a "DNA decoder" step must:
  1. Delete any pre-existing reports in `build/reports/`, if present
  2. Read the DNA file
  3. Generate a fresh report
- The same regeneration must happen as **step 1 of every iteration** (Section 6.1) — the
  report always reflects the current state of the DNA and the code generated in the
  previous iteration.

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

## 6. Iteration Algorithm

Each iteration, performed in order by the operator/agent via Cursor:

### 6.1 Regenerate the DNA report
Invoke the fixed-API DNA reader/report class to rebuild the report based on the DNA file
and the code as it exists after the previous iteration.

### 6.2 Read the spec
Read `docs/tz.md` (this document) in full.

### 6.3 Read the decision log
Read `docs/log.md` to understand the history of prior decisions and their rationale.

### 6.4 Decide on a code change
Based on the freshly generated report, the log, and the spec, decide what change to make
to the codebase for this iteration. The DNA is treated as a **growth rule** — i.e., a
constraint/inspiration for how the code should be patched or extended, not a literal
translation table. The scope of a single iteration's change should be small and reviewable
(one coherent step of growth, not a rewrite).

Explicitly consider **both surfaces** when deciding: does this iteration's growth belong on
the web UI, the MCP server, or both? A new capability discovered from the DNA/report should
generally be added to the shared underlying class (5.1/5.2/etc.) and then exposed through
whichever surface(s) make sense — not silently added to only one surface by default.

### 6.5 Record the decision
Append an entry to `docs/log.md` describing:
- The iteration number
- What was decided and why (tie it back to the report/DNA input where relevant)
- What was changed in the code

### 6.6 Commit
Commit the change to Git (**no push**). Commit message format:

```
Iteration #<index> - <brief description>
```

### 6.7 Report to the operator
Output a summary message describing what was done this iteration (what changed, why, and
any notable observations from the report).

### 6.8 Module growth
As the application grows, it may be split into modules and sub-modules. When this happens:
- Each module/sub-module gets **its own DNA-derived report** and **its own patch/iteration
  history** going forward, scoped to that module
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
├── build/
│   └── reports/           generated reports (git-ignored)
├── src/
│   └── main/java/...      application code (dna / report / storage / web / mcp packages)
├── .gitignore             must exclude build/reports/
├── build.gradle.kts
└── settings.gradle.kts
```

## 8. Open Points / Assumptions Made

- **DNA-to-code mapping logic** is intentionally left undefined here — by design, this is
  decided fresh by the LLM at each iteration based on the report, log, and spec, not
  specified as a fixed algorithm.
- **Report content/format** is intentionally left open-ended per Section 5.2 — the fixed
  API must support an evolving report structure rather than assuming a fixed schema.
- **MCP tool/resource shape** is intentionally left open-ended per Section 5.5, for the same
  reason — grows iteration by iteration rather than being fixed upfront.
- **Specific MCP library/SDK** for the Java/Spring Boot side is not pinned — chosen and
  recorded in `docs/log.md` when the first MCP-related iteration happens.
- **Stopping condition** for the overall iterative growth process is not defined — the
  process continues for as long as the operator chooses to run further iterations.