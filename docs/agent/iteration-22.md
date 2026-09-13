# Agent instructions — iteration 22

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_rows`) output as a verdict.

Previous instruction file: `docs/agent/iteration-21.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`rows`**:

| Key | Meaning for an agent |
| --- | --- |
| `lineCount` | Newline-delimited lines, matching `reflow.sourceLines`. |
| `scanned` | Non-blank lines used as records. |
| `tileLength` | Prefix width (16). |
| `uniqueShareAt16` | Distinct 16-character line prefixes / scanned. |
| `uniqueAt` | Smallest prefix k≥8 that uniquely IDs every non-blank line, or 0. |
| `twinCount` / `twinLines` | Colliding 16-prefixes and how many lines they cover. |
| `topPrefix` / `topCount` | The most repeated line prefix (often a timestamp). |

**Do not** drop newlines before calling this. If `lanes.spread ≤ 0.02`, uniqueness
geography on concatenated 70-frames is flat — read `rows` instead of inventing a
fingerprint column.

MCP follow-up: `profile_rows` lists the line-native summary. `profile_lanes` still
drops newlines; `find_forks` / `measure_key_width` also drop newlines.

On documents (`docs/tz.md`), a line is a markdown/spec line, not a wrap-70 FASTA
frame. `uniqueShareAt16` near 1.0 means headings and sentences already identify
themselves; twins are repeated boilerplate.

## Verification criterion (this iteration)

**Pass if all of:**

1. `rows` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `rows` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `tileLength=16`.
3. On **logs-after**, `rows.lineCount` equals `reflow.sourceLines`.
4. On **logs-after**, `|rows.uniqueShareAt16 − forks.uniqueShare| > 0.05` —
   line-native 16-mers are not a restatement of concatenated wrap-frame forks.

**Fail if** the two unique-shares match within 0.05 (still the glued tape), even
when the `rows` section exists. Quote `lineCount`, `uniqueShareAt16`, and
`forks.uniqueShare` from both after files when scoring.

## Check the previous step (iteration 21)

Re-check iteration 21 against **logs-before** (old facade, has `lanes`, no `rows`):

- Hypothesis claimed a fingerprint column when `lanes.spread > 0.02`.
- **Must still hold on logs-before:** `lanes.spread ≤ 0.02` (the FAIL recorded as
  a Con). Quote `troughAt`, `peakAt`, and `spread`. If logs-before instead shows
  spread > 0.02, the previous verdict drifted and must be flagged.
- `lanes.troughShare` should still equal `forks.uniqueShare` (the restatement Con).
- Do **not** treat a flat `spread` as a logging-quality failure; it means “no
  column to prefer on concatenated wrap-frames.” Use `rows` (this iteration)
  rather than repeating a five-column lane profile.
