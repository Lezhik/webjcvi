# Agent instructions — iteration 26

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_majorities`) output as a verdict.

Previous instruction file: `docs/agent/iteration-25.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`majorities`**:

| Key | Meaning for an agent |
| --- | --- |
| `majorityAt` | Smallest prefix length k with uniqueShare(k) ≥ 0.5. 0 if uniqueness never covers half the lines (k≤256). |
| `shareAtMajority` | Unique share at `majorityAt` (0 if none). |
| `riseAt` / `shareAtRise` | Steepest uniqueness jump from iteration 25 — a start column, not necessarily an identifier. |
| `pastRise` | True when `majorityAt > riseAt`. Then start an identifier at `majorityAt`, not at `riseAt`. |
| `lag` | `majorityAt − riseAt` when `pastRise`; how far past the steepest jump until a majority. |
| `threshold` | Always 0.5. |
| `topPrefix` / `topCount` | Clock veto and colliding prefix. |

If `rises.shareAtRise < 0.5`, **do not** treat `riseAt` as an identifier — read
`majorities.pastRise`. A steep jump that still leaves most lines colliding is layout,
not a key.

MCP follow-up: `profile_majorities`. `profile_rises` remains the steepest-gain column.

On documents, `majorityAt=8` and `pastRise=false` means uniqueness already covers a
majority at the floor (headings, fences, short distinct lines) — same as DNA.

## Verification criterion (this iteration)

**Pass if all of:**

1. `majorities` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `majorities` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `floorLength=8` and `threshold=0.5`.
3. On **logs-after**, `majorities.majorityAt > 16` and `majorities.pastRise` is true —
   a majority identifier sits past the steepest rise, even if `shareAtRise` is below 0.5.

**Fail if** logs-after `majorityAt=0` or `majorityAt ≤ riseAt`, even when the
`majorities` section exists. Quote `majorityAt`, `shareAtMajority`, `riseAt`,
`shareAtRise`, `lag`, and `pastRise` from both after files when scoring.

## Check the previous step (iteration 25)

Re-check iteration 25 against **logs-before** (old facade, has `rises`, no `majorities`):

- Hypothesis claimed `riseAt > 16` / `pastClock=true` as the identifier start when
  uniqueness never cliffs.
- **Must still hold on logs-before:** `rises.riseAt > 16` and `rises.pastClock=true`.
  Quote `riseAt`, `gain`, `shareAtRise`, `cliffAt`, `pastClock`.
- **Must still hold as a Con:** `rises.shareAtRise < 0.5` — the steepest jump is not
  yet a majority identifier. Use `majorities.majorityAt` (this iteration) rather than
  treating `riseAt` as a key.
