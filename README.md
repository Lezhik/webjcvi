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
│   └── log.md             append-only iteration decision log
├── build/
│   └── reports/           generated DNA/code reports (git-ignored, regenerated per iteration)
│       └── dna/           DNA decoder output (`dna-report.md`)
├── src/
│   └── main/java/...      application code (dna / report / storage / web / mcp packages)
├── AGENTS.md              operating instructions for AI coding agents working in this repo
├── build.gradle.kts
└── settings.gradle.kts
```

## How the project grows

Growth happens through a fixed iteration algorithm, run manually by the operator (currently
via Cursor). Each iteration:

1. Regenerates the DNA-derived report from the current DNA + codebase
2. Reads `docs/tz.md` and `docs/log.md`
3. Decides on one small, reviewable code change — on the web side, the MCP side, or both
4. Logs the decision in `docs/log.md`
5. Commits locally as `Iteration #<index> - <brief description>`

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
- Generated reports (`build/reports/`) are never committed to Git.
- Automated tests cover sandboxing/security, core DNA-parsing and report-generation
  functionality, and size/edge-case limits — for both the web and MCP surfaces.

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
  Tools: `regenerate_dna_report`, `read_dna_report`, `list_files`, `read_file`.

The DNA decoder (`generateDnaReport`) is also step 1 of every later iteration.
