# Agent instructions — iteration 27

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_nears`) output as a verdict.

Previous instruction file: `docs/agent/iteration-26.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`nears`**:

| Key | Meaning for an agent |
| --- | --- |
| `nearAt` | Smallest prefix length k with uniqueShare(k) ≥ 0.90. 0 if uniqueness never reaches 0.90 (k≤256). |
| `shareAtNear` | Unique share at `nearAt` (0 if none). |
| `majorityAt` / `shareAtMajority` | Half-unique width from iteration 26 — not yet a near-unique key when share ≈ 0.55. |
| `pastMajority` | True when `nearAt > majorityAt`. Then start an identifier at `nearAt`, not at `majorityAt`. |
| `lag` | `nearAt − majorityAt` when `pastMajority`; how far past half-unique until near-unique. |
| `threshold` | Always 0.90. Softer than the 0.99 cliff that logs often never reach. |
| `topPrefix` / `topCount` | Clock veto and colliding prefix. |

If `majorities.shareAtMajority < 0.90`, **do not** treat `majorityAt` as an identifier —
read `nears.pastMajority`. A 0.5-width that still leaves ~45% of lines colliding is not a
key. Do **not** wait for `cliffAt` (0.99); use 0.90 when the cliff is missing.

MCP follow-up: `profile_nears`. `profile_majorities` remains the 0.5-width.

On documents, `nearAt=8` and `pastMajority=false` means uniqueness already covers 90% at
the floor (headings, fences, short distinct lines).

## Verification criterion (this iteration)

**Pass if all of:**

1. `nears` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `nears` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `floorLength=8` and `threshold=0.90`.
3. On **logs-after**, `nears.nearAt > 16` and `nears.pastMajority` is true — a
   near-unique identifier sits past the 0.5 majority, even if `cliffAt` is 0.

**Fail if** logs-after `nearAt=0` or `nearAt ≤ majorityAt`, even when the `nears`
section exists. Quote `nearAt`, `shareAtNear`, `majorityAt`, `shareAtMajority`,
`lag`, and `pastMajority` from both after files when scoring.

## Check the previous step (iteration 26)

Re-check iteration 26 against **logs-before** (old facade, has `majorities`, no `nears`):

- Hypothesis claimed `majorityAt > 16` / `pastRise=true` as the identifier start when
  the steepest jump still left most lines colliding.
- **Must still hold on logs-before:** `majorities.majorityAt > 16` and
  `majorities.pastRise=true`. Quote `majorityAt`, `shareAtMajority`, `riseAt`,
  `shareAtRise`, `lag`, `pastRise`.
- **Must still hold as a Con:** `majorities.shareAtMajority < 0.90` — half-unique is
  not a near-unique key. Use `nears.nearAt` (this iteration) rather than treating
  `majorityAt` as a key, and rather than waiting for a 0.99 cliff that may never arrive.
