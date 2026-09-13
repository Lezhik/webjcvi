# WebJCVI

An experiment in "growing" an application, iteration by iteration, out of a real DNA
sequence — using AI agents as the decision-making and code-generation engine.

## What this is

A DNA file (`jcvi-dna.txt`, project root) is treated as a **growth ruleset**. At each
iteration, an AI agent regenerates a report from the DNA and the current codebase, reads the
spec and the decision history, and decides how to extend the application. The DNA is not a
literal spec — it's reinterpreted freely each time as inspiration/constraint for the next
patch, by design.

The application has **two surfaces**:
- A **web application** (Spring WebFlux + JTE) for human viewing and interaction
- An **MCP (Model Context Protocol) server**, exposing the same capabilities as MCP
  tools/resources so other AI agents can call into it directly

The product being grown is **document and log analysis for AI agents**: a shared
text-analysis facade (JSON over caller-supplied text) plus committed instruction files
under `docs/agent/` that tell an agent how to use those reports and how to check its
previous step. DNA remains the growth ruleset; it is not the payload the tools analyze.

Both surfaces are built on the same shared underlying classes — nothing is duplicated
between them. There is no database; the app is file-based only, sandboxed to its own project
directory.

This is a research project. There is no fixed roadmap — the codebase's shape and features
emerge from the iteration loop itself.

## Tech stack

- Java, Spring Boot, Spring WebFlux (reactive)
- JTE for server-rendered views
- MCP server for AI-agent access (library/SDK choice recorded in `docs/log.md`)
- Gradle with the Kotlin DSL (`build.gradle.kts`), single module for now
- No database — file-based storage only, capped at 500 MB per file, sandboxed to the
  project directory
- Git for version control (iterations commit locally; no automated push)

## Project structure

```
/
├── jcvi-dna.txt           DNA input file (read-only from the app's perspective)
├── docs/
│   ├── tz.md              full technical specification
│   ├── log.md             append-only iteration decision log
│   └── agent/             agent instructions per iteration (committed, never overwritten)
├── reports/
│   └── logs/              test journal + four JSON analyses (git-ignored)
├── build/
│   └── reports/           generated DNA/code reports (git-ignored, regenerated per iteration)
│       └── dna/           DNA decoder output (`dna-report.md`)
├── src/
│   └── main/java/...      application code (dna / report / storage / web / mcp / logs)
├── AGENTS.md              operating instructions for AI coding agents working in this repo
├── build.gradle.kts
└── settings.gradle.kts
```

## How the project grows

Growth happens through a fixed iteration algorithm, run manually by the operator (currently
via Cursor). Each iteration:

1. Regenerates the DNA-derived report from the current DNA + codebase
2. Reads the **previous** `docs/log.md` hypothesis and its Pros/Cons first, then the rest
   of the log and `docs/tz.md`
3. Forms a **four-part hypothesis** (DNA/text rule, usefulness for documents/logs,
   practical use, verification criterion)
4. Captures **old** JSON analyses of `test.log` and `docs/tz.md` (before code change)
5. Makes one small, reviewable code change (web, MCP, or both), **keeps** the previous
   text-analysis algorithm, and writes new `docs/agent/iteration-<n>.md` instructions
6. Runs tests, captures **new** JSON analyses of logs and `docs/tz.md`
7. Compares old vs new (and reads both instruction files plus the criterion) **before**
   writing Pros/Cons
8. Evolves the DNA report builder, appends `docs/log.md`, commits locally as
   `Iteration #<index> - <brief description>`

As the app grows, it may split into modules/sub-modules, each with its own scoped report and
patch history.

Full details of the algorithm and every requirement live in [`docs/tz.md`](docs/tz.md).
If you're an AI agent about to work in this repo, read [`AGENTS.md`](AGENTS.md) first —
it's the operating procedure; `docs/tz.md` is the source of truth if the two ever disagree.

## Key invariants (see `docs/tz.md` for full detail)

- All file I/O goes through a single sandboxed storage class — no component touches the
  filesystem directly, and neither the web UI nor the MCP server can escape the project
  directory or the 500 MB size limit.
- DNA reading and report generation goes through a single fixed-API class; the *content* of
  the report evolves iteration by iteration, decided by the agent, not hard-coded.
- Text analysis (logs **and** documents) goes through a single facade; previous algorithms
  stay. Agent instructions under `docs/agent/` are committed and never overwritten.
- Generated reports (`build/reports/`, `reports/logs/`) are never committed to Git.
- Automated tests cover sandboxing/security, core DNA-parsing and report-generation
  functionality, and size/edge-case limits — for both the web and MCP surfaces.
- MCP clients must read the current `docs/agent/iteration-*.md` and apply its previous-step
  criteria before treating analysis JSON as a verdict.

## Status

Preparation complete: sandboxed storage, DNA report pipeline, WebFlux+JTE UI, and MCP
tools are in place. See `docs/log.md` for the current iteration history.

## Running

Requires JDK 21+ (the build produces Java 21 bytecode).

```
./gradlew test
./gradlew generateDnaReport
./gradlew bootRun
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

- Web UI: `http://localhost:8080/`
- Report page: `http://localhost:8080/report`
- HTTP API: `GET /api/files`, `GET /api/files/content?path=...`, `GET /api/report`,
  `POST /api/report/regenerate`
- MCP: Spring AI WebFlux SSE server (see `spring.ai.mcp.server` in `application.yml`).
  File tools (`list_files`, `read_file`) and text tools (including `analyze_logs`) share
  the same sandbox. Before interpreting analysis JSON, an MCP client should `read_file`
  the latest `docs/agent/iteration-*.md` and apply that file's verification criteria.

The DNA decoder (`generateDnaReport`) is also step 1 of every later iteration.
