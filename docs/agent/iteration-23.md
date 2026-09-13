# Agent instructions — iteration 23

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_cliffs`) output as a verdict.

Previous instruction file: `docs/agent/iteration-22.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`cliffs`**:

| Key | Meaning for an agent |
| --- | --- |
| `forkAt` | 1-based first column where the largest colliding 16-prefix splits. Start an identifier here, not at column 1. |
| `cliffAt` | Smallest prefix k≥8 whose unique share is ≥ 0.99, or 0 if uniqueness never cliffs. |
| `shareAt16` | Unique share of 16-character line prefixes (the clock veto from iteration 22). |
| `shareAtCliff` | Unique share at `cliffAt` (0 if no cliff). |
| `topPrefix` / `topCount` | The colliding prefix (often a timestamp) and how many lines share it. |

If `rows.uniqueAt` is 0, **do not** stop at the 16-character veto — read `forkAt`.
If `forkAt > 16`, the clock occupies columns 1..16; slice records from `forkAt`.
`find_forks` still drops newlines and is a different object.

MCP follow-up: `profile_cliffs`. `profile_rows` remains the 16-veto.

On documents, `forkAt` is where repeated boilerplate (fences, headings) first
differs; `cliffAt=8` means lines are already unique at the floor.

## Verification criterion (this iteration)

**Pass if all of:**

1. `cliffs` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `cliffs` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `tileLength=16`.
3. On **logs-after**, `cliffs.forkAt > 16` — the split sits past the clock, so an
   agent can start an identifier after column 16.

**Fail if** logs-after `forkAt ≤ 16` or `forkAt=0` (still inside the 16-veto, or
identical lines), even when the `cliffs` section exists. Quote `forkAt`,
`cliffAt`, and `shareAt16` from both after files when scoring.

## Check the previous step (iteration 22)

Re-check iteration 22 against **logs-before** (old facade, has `rows`, no `cliffs`):

- Hypothesis claimed line-native 16-mers differ from wrap-frame forks.
- **Must still hold on logs-before:** `|rows.uniqueShareAt16 − forks.uniqueShare| > 0.05`
  and `rows.lineCount` equals `reflow.sourceLines`. Quote those four numbers.
- `rows.uniqueAt` should still be 0 and `topPrefix` a timestamp — the void that
  iteration 22 recorded as a Con.
- Do **not** treat `uniqueAt=0` as a logging-quality failure; it means “no
  saturating line key.” Use `cliffs.forkAt` (this iteration) rather than repeating
  a 16-character unique-share veto.
