# WebJCVI decision log

This file is append-only. Each iteration adds a new section; earlier entries are never edited or deleted.

---

## Preparation — project bootstrap

**Date:** 2026-09-11

**Decision.** Stand up the dual-surface skeleton required by `docs/tz.md` before the iteration loop begins: sandboxed file storage, a fixed-API DNA reader/report builder, a WebFlux+JTE web UI, an MCP tool surface on the same classes, and the automated tests in §5.4. The DNA decoder step (TZ §5.3) regenerates `build/reports/` from `jcvi-dna.txt` plus a codebase snapshot.

**Why this shape.**
- All file I/O is confined to `FileStorageService` (project-root sandbox, symlink-aware, 500 MB limit on read and write, DNA file treated as read-only). No other production class uses `java.nio.file.Files` or `FileInputStream`/`FileOutputStream`; ArchUnit enforces that.
- `DnaReportService.regenerate()` is the stable API for report lifecycle (clear prior reports, parse DNA, write markdown). Report *content* is intentionally a first cut: composition tables, IUPAC/invalid handling notes, a source-file snapshot, and growth notes. Later iterations may change the markdown without changing the method signatures.
- Ambiguous IUPAC characters are kept in the normalized sequence and counted separately; any other non-whitespace character is dropped from the sequence and counted as invalid so parsing never crashes (TZ §4.1).
- Both surfaces call the same services. Web: HTML pages plus `/api/files` and `/api/report`. MCP: `regenerate_dna_report`, `read_dna_report`, `list_files`, `read_file`.
- Blocking filesystem work on the web side is offloaded to `Schedulers.boundedElastic()`.

**MCP library.** `org.springframework.ai:spring-ai-starter-mcp-server-webflux:1.1.2` (SSE over WebFlux, Spring Boot 3.5 compatible). Tools are registered with `MethodToolCallbackProvider` from `@Tool` methods on `WebJcviMcpTools`. Spring AI 2.x was skipped because it requires Spring Boot 4 and would conflict with JTE's Boot 3 starter.

**Stack pins.** Java 21 bytecode, Spring Boot 3.5.15, Spring WebFlux, JTE 3.2.4 (`jte-spring-boot-starter-3`), Gradle Kotlin DSL, single module. No database.

**Tests.** Sandbox/size-limit coverage on `FileStorageService`, on the HTTP API, and on the MCP tool class; DNA parser edge cases; report regeneration clearing prior output; ArchUnit bypass checks. The 500 MB boundary is exercised through an injected smaller limit that uses the same comparison, plus an assertion that the production constant is `500L * 1024 * 1024`.

**Module split.** Not applicable — this is the initial single-module layout (TZ §6.8).

**DNA decoder.** After the test suite, `./gradlew generateDnaReport` (class `GenerateReportMain`) deletes `build/reports/dna/` contents and writes a fresh `build/reports/dna/dna-report.md`. DNA reports live under `build/reports/dna/` so regeneration does not wipe Gradle's own `build/reports/tests`. Generated reports stay git-ignored via `build/`.
