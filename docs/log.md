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

---

## Iteration #1
- **Hypothesis:** The DNA encodes a *working-tape protocol* for arbitrary text, not a spec for DNA analysis and not a spec for this repository. Concrete signals from the v1 composition report: (R1) 0 ambiguous and 0 invalid bases — identity is fold-then-exact; (R2) A=199831 ≈ T=203352 (|A−T|=3521 of 531490) — one linear complementary buffer, not a graph; (R3) the 80-base preview contains poly-T (`TTTTT`) and poly-A (`AAAAA`) — consecutive identical symbols are a native delimiter; (R4) 7593 wrapped source lines — hits carry a line offset. Therefore the useful system is an in-memory **scratch tape**: a caller pastes any document, then exact-finds a motif and lists character runs. Payload is caller text; the DNA is only the protocol.
- **Functionality changes:** Shared class `ScratchTape` (no `FileStorageService`, no `jcvi-dna.txt`). Web: `/tape` load/find/runs UI; `POST /api/tape`, `GET /api/tape/find`, `GET /api/tape/runs`. MCP: `tape_load`, `tape_find`, `tape_runs`. Both surfaces.
- **Pros:** R1 and R4 map cleanly onto grep-like search of a pasted buffer, which agents actually need (search a log or draft without writing a project file). R3 maps onto run listing. The capability is independent of DNA content and of repo paths.
- **Cons:** Chargaff pairing (R2) is used only as “stay linear,” which is weak — A≈T is not really exercised by find/runs. Run structure was inferred from an 80-base preview, not measured on the whole tape. A paste-grep is useful but thin; it does not yet explain GC rarity or AT-richness beyond “keep the bound small” (524288 ≈ sequence length).
- **Report builder changes:** Replaced the bag-of-bases-only builder with wrap-frame census (min/median/mode/max line width of the raw FASTA wrapping) plus a whole-tape homopolymer profile (max run per base; buckets 5–9 / 10–19 / 20+). This addresses the cons: the next iteration sees measured runs and frame size instead of eyeballing the preview, so a stronger hypothesis can use those numbers rather than restating “the tape is linear.”

---

## Iteration #2
- **Hypothesis:** The DNA encodes a *record-separator protocol* for arbitrary text streams, not a grep tape and not DNA analysis. Concrete signals from the v2 wrap+homopolymer report: (R1) 8114 runs of length ≥ 5 but only 9 of length 10–19 and 1 of length 20+ — long homopolymers are rare banners, short ones are payload noise; (R2) longest A×22 and T×18, while G and C max out at 7 — the separator class is the abundant AT alphabet, never GC; (R3) wrap modal/median/max width 70 — frames are almost uniform. Therefore the useful system is a **banner splitter**: the caller pastes a log, email, or dump, and the app cuts it into sections on unusually long identical-character runs (`==========`, `----------`, `AAAAAAAAAA`). Default min-run 10 is the measured rare class. Payload is caller text; the DNA is only the delimiter rule.
- **Functionality changes:** Shared class `BannerSplitter` (no `FileStorageService`, no `jcvi-dna.txt`). Web: `/split` form UI; `POST /api/split`. MCP: `split_banners`. Both surfaces. Scratch tape from iteration #1 is kept (small step; this iteration adds sectioning rather than replacing search).
- **Pros:** R1 maps onto a real agent need: cutting pasted logs and email dumps on banner lines without writing a project file. Default 10 is taken from the measured 10+ rarity, not from the 80-base preview. The capability is independent of DNA content and of repo paths.
- **Cons:** Chargaff pairing is still not exercised by the splitter — A≈T only loosely justifies “use the abundant class as delimiters.” GC’s max run of 7 is noted but the splitter treats any long run (`====`) as a banner, not only AT. Wrap width 70 is unused by the product (only by the next report builder). A banner cut is useful but still a thin protocol; it does not yet use local pairing inside sections.
- **Report builder changes:** Replaced the wrap-histogram + homopolymer-bucket lead with **frame-window Chargaff skew**: walk the normalized tape in windows of modal wrap width, report mean/max |AT-skew| and |GC-skew|, and count windows whose |AT-skew| exceeds 0.05 (above the global residual ~0.009). Wrap census shrinks to “window source”; homopolymers shrink to a remainder line. This is a different reading (local pairing along frames) so the next hypothesis can use failing windows rather than restating “long runs are delimiters.”

---

## Iteration #3
- **Hypothesis:** The DNA encodes a *local pair-drift protocol* for arbitrary text, not a grep tape and not a banner splitter. Concrete signals from the v3 frame-window Chargaff report: (R1) global A=199831 ≈ T=203352 (|A−T|=3521, residual ~0.009) while **6108 of 7593** 70-base windows have |AT-skew| > 0.05 (mean 0.1515, max 0.7308) — pairing is an accounting identity, the payload is the drifted frames; (R2) window size is the modal wrap **70**; (R3) G↔C is the rarer complementary class and is even more locally skewed (mean |GC-skew| 0.257). Therefore the useful system is a **pair-drift scanner**: the caller pastes code, JSON, or a log, the app walks it in 70-char frames, and lists windows where complementary brackets `()[]{}<>` locally unbalance even when the whole paste still sums to ~zero leftover. Default threshold 0.05 is the measured local-failure floor. Payload is caller text; the DNA is only the pairing-vs-window rule.
- **Functionality changes:** Shared class `PairDrift` (no `FileStorageService`, no `jcvi-dna.txt`). Web: `/drift` form UI; `POST /api/drift`. MCP: `pair_drift`. Both surfaces. Scratch tape and banner splitter from #1–#2 are kept.
- **Pros:** R1 maps onto a real agent need: finding locally unmatched brackets in a pasted dump without writing a project file, including cases that globally still “balance.” Window 70 and threshold 0.05 are taken from the v3 census, not invented. The capability is independent of DNA content and of repo paths.
- **Cons:** Mapping A↔T onto `()`/`[]`/`{}`/`<>` is analogical, not literal — the DNA has no brackets. 80% of DNA windows fail, so “hotspot” in DNA is the majority class, while in code a hotspot is the bug; the polarity of the signal is stretched. Wrap joints and island geography were unused by the product (they become the next report). A windowed bracket tally is useful but still a thin protocol; it does not nest-match or report *which* pair class drifted.
- **Report builder changes:** Replaced the per-window mean/max skew table with **skew-island geography** (consecutive fail/pass windows compressed into islands: count, longest fail, longest pass) plus a **wrap-joint dinucleotide census** (last base of line N glued to first of line N+1; complementary AT/TA/GC/CG share; top dimers). Aggregate Chargaff and homopolymers shrink to a remainder line. This is a different reading (geography of drift + glue between frames) so the next hypothesis can use clustering and boundary dimers rather than restating “local pairing fails.”
