# Agent instructions — iteration 21

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_lanes`) output as a verdict.

Previous instruction file: **none** (first committed agent file). Treat a missing
baseline as: there is no prior verification criterion to re-check except the
iteration-20 Pros/Cons in `docs/log.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`lanes`**:

| Key | Meaning for an agent |
| --- | --- |
| `tileLength` | Fingerprint width (16). |
| `troughAt` | Start column where 16-mers collide most. Do **not** index records from here. |
| `peakAt` | Start column where 16-mers are most unique. Prefer this as a fingerprint. |
| `troughShare` / `peakShare` | Unique-share at those columns (1.0 = every frame distinct). |
| `spread` | `peakShare - troughShare`. Near 0 means uniqueness is flat (no column to prefer). |

Also inspect `affixes.cheaperEnd` (lead vs tail saturating uniqueAt) and
`forks.uniqueShare` (leading 16-mer collisions). If `cheaperEnd` is `none` and
`keys.uniqueAt` is 0, **do not** invent a saturating key — read `lanes` instead.

MCP follow-up: `profile_lanes` lists per-column shares; `compare_affixes` is only
the two ends; `find_forks` lists leading twins, not a geography.

On documents (`docs/tz.md` or similar), wrap-frames are an analog of 70-wide
records after dropping newlines. `peakAt` still means “cheapest fingerprint
column in that analog,” not a markdown heading offset.

## Verification criterion (this iteration)

**Pass if all of:**

1. `lanes` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `lanes` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `tileLength=16` and `wrapWidth=70`.
3. On **logs-after**, `lanes.spread > 0.02` — uniqueness is not flat, so an agent
   can prefer `peakAt` over `troughAt` as a fingerprint column.

**Fail if** `spread ≤ 0.02` on logs-after (uniform collisions, no column to prefer),
even if the `lanes` section exists. Quote `troughAt`, `peakAt`, and `spread` from
both after files when scoring.

## Check the previous step (iteration 20)

Re-check iteration 20 against **logs-before** (old facade, no `lanes`):

- Hypothesis claimed a cheaper saturating end (`cheaperEnd` lead/tail/tie).
- **Must still hold on logs-before:** `affixes.cheaperEnd` is `"none"` and both
  `leadUniqueAt` and `tailUniqueAt` are 0 — the void that iteration 20 recorded
  as a Con. If logs-before instead shows `lead` or `tail`, the previous verdict
  drifted and must be flagged.
- `affixes.leadShareAtNear` should still approximately equal `forks.uniqueShare`
  (the restatement Con).
- Do **not** treat `cheaperEnd=none` as a logging-quality failure; it means “no
  saturating affix on this journal.” Use `lanes` (this iteration) rather than
  repeating a two-end uniqueAt search.
