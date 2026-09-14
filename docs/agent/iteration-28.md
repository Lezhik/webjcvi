# Agent instructions — iteration 28

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_residues`) output as a verdict.

Previous instruction file: `docs/agent/iteration-27.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`residues`**:

| Key | Meaning for an agent |
| --- | --- |
| `nearAt` / `shareAtNear` | Near-unique 0.90-width from iteration 27 — not yet a complete key when ~8% still collide. |
| `residueShare` | Share of lines that still share a prefix of length `nearAt` with at least one other line. |
| `twinGroups` / `twinLines` | Leftover colliding groups at `nearAt`. |
| `minSplit` / `modalSplit` / `maxSplit` | First-difference columns of those leftover twins. 0 means identical lines. |
| `splitLag` | `modalSplit − nearAt` when the leftover forks past the 0.90-width. |
| `stretched` | True when `splitLag > 2` (DNA leftover vanishes in two bases: nearAt=10 → cliffAt=12). Then do **not** treat `nearAt` as an identifier for residual lines — start at `modalSplit`. |
| `topPrefix` / `topCount` | Largest leftover group at k=`nearAt`. |

If `nears.shareAtNear < 1.0`, **do not** treat `nearAt` as unique for every line —
read `residues.stretched`. A 0.90-width that still leaves a colliding tail is not a
key for those leftovers. Do **not** wait for `cliffAt` (0.99); profile the residue
instead of raising the unique-share threshold again.

MCP follow-up: `profile_residues`. `profile_nears` remains the 0.90-width.

On documents, `twinGroups=0` and `stretched=false` means uniqueness at `nearAt` already
has no leftover twins (short distinct headings). Identical fence lines may form twins
that never split (`modalSplit=0`).

## Verification criterion (this iteration)

**Pass if all of:**

1. `residues` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `residues` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `floorLength=8`.
3. On **logs-after**, `residues.twinGroups > 0` and `residues.stretched` is true
   (`splitLag > 2`) — leftover collisions after 0.90 are not DNA-tight.

**Fail if** logs-after `twinGroups=0` or `stretched` is false, even when the
`residues` section exists. Quote `nearAt`, `shareAtNear`, `residueShare`,
`twinGroups`, `twinLines`, `modalSplit`, `splitLag`, and `stretched` from both
after files when scoring.

## Check the previous step (iteration 27)

Re-check iteration 27 against **logs-before** (old facade, has `nears`, no `residues`):

- Hypothesis claimed `nearAt > 16` / `pastMajority=true` as the identifier start when
  the 0.5 majority still left most lines colliding.
- **Must still hold on logs-before:** `nears.nearAt > 16` and
  `nears.pastMajority=true`. Quote `nearAt`, `shareAtNear`, `majorityAt`,
  `shareAtMajority`, `lag`, `pastMajority`.
- **Must still hold as a Con:** `nears.shareAtNear < 0.99` and uniqueness still has
  no 0.99 cliff — 0.90 is not 0.99. Use `residues` (this iteration) rather than
  treating `nearAt` as a complete key, and rather than waiting for a 0.99 cliff
  that may never arrive.
