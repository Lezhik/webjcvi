# Agent instructions — iteration 25

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_rises`) output as a verdict.

Previous instruction file: `docs/agent/iteration-24.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`rises`**:

| Key | Meaning for an agent |
| --- | --- |
| `riseAt` | Prefix length k maximizing uniqueShare(k) − uniqueShare(k−1). The steepest uniqueness jump. |
| `gain` | That delta (0 if no rise). |
| `shareAtRise` | Unique share at `riseAt`. |
| `pastClock` | True when `riseAt > 16`. Then start an identifier at `riseAt` even if `cliffAt` is 0. |
| `cliffAt` | Smallest k with unique share ≥ 0.99, or 0 if uniqueness never cliffs (iteration 24 void). |
| `shareAt16` / `topPrefix` | Clock veto and colliding prefix. |

If `runways.stretched` is false because `cliffAt=0`, **do not** stop — read `rises.pastClock`.
A missing 0.99 cliff is not “no identifier”; it means use the steepest rise instead.

MCP follow-up: `profile_rises`. `profile_runways` remains the cliff-minus-fork gap.

On documents, `riseAt=8` and `pastClock=false` means uniqueness already jumps at the floor
(headings, fences, short distinct lines).

## Verification criterion (this iteration)

**Pass if all of:**

1. `rises` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `rises` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `floorLength=8`.
3. On **logs-after**, `rises.riseAt > 16` and `rises.pastClock` is true — the steepest
   uniqueness jump sits past the clock, even if `cliffAt` is 0.

**Fail if** logs-after `riseAt ≤ 16` or `riseAt=0`, even when the `rises` section exists.
Quote `riseAt`, `gain`, `shareAtRise`, `cliffAt`, and `pastClock` from both after files
when scoring.

## Check the previous step (iteration 24)

Re-check iteration 24 against **logs-before** (old facade, has `runways`, no `rises`):

- Hypothesis claimed a stretched runway (`cliffAt − forkAt > 20`) as the identifier body.
- **Must still hold on logs-before as a Con:** `runways.runway=0` and
  `runways.stretched=false` (and typically `cliffAt=0`). Quote those three numbers plus
  `forkAt`.
- Do **not** treat a missing 0.99 cliff as a logging-quality failure. Use `rises.riseAt`
  (this iteration) rather than repeating a runway subtraction that is empty without a cliff.
