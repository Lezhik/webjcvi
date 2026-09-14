# Agent instructions — iteration 29

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_dups`) output as a verdict.

Previous instruction file: `docs/agent/iteration-28.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`dups`**:

| Key | Meaning for an agent |
| --- | --- |
| `nearAt` / `shareAtNear` / `residueShare` | Near-unique leftover from iterations 27–28. |
| `twinGroups` / `twinLines` | Leftover colliding groups at `nearAt`. |
| `copyGroups` / `copyLines` | Leftover groups whose members are identical (forkAt=0). Dedupe these. |
| `forkGroups` / `forkLines` | Leftover groups that still differ past `nearAt`. |
| `copyShare` | `copyLines / twinLines`. DNA leftover is 0 (all forks). |
| `mostlyCopies` | True when `copyShare > 0.5`. Then leftover is mostly duplicate lines — dedupe rather than walking `splitLag`. |
| `minFork` | First-difference column of leftover *forks* (0 if none). |
| `topPrefix` / `topCount` | Largest leftover **copy** group. |

If `residues.stretched` is false because `modalSplit=0`, **do not** treat that as a void —
read `dups.copyShare`. Identical DEBUG lines are the leftover class to strip. Do **not**
raise the unique-share threshold or wait for `cliffAt` (0.99).

MCP follow-up: `profile_dups`. `profile_residues` remains the stretch/splitLag readout.

On documents, `copyGroups` on identical fences (` ``` `) means those lines are copies, not
a layout body. `copyShare=0` with `forkGroups>0` matches DNA leftover (twins that still
fork).

## Verification criterion (this iteration)

**Pass if all of:**

1. `dups` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `dups` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `floorLength=8`.
3. On **logs-after**, `dups.copyGroups > 0` and `dups.copyShare > 0` — leftover after
   0.90 includes exact duplicate lines, unlike DNA copyShare=0.

**Fail if** logs-after `copyGroups=0` or `copyShare=0`, even when the `dups` section
exists. Quote `nearAt`, `copyGroups`, `copyLines`, `forkGroups`, `forkLines`,
`copyShare`, `mostlyCopies`, and `minFork` from both after files when scoring.

## Check the previous step (iteration 28)

Re-check iteration 28 against **logs-before** (old facade, has `residues`, no `dups`):

- Hypothesis claimed leftover after 0.90 is a stretched split (`twinGroups > 0` and
  `stretched=true` / `splitLag > 2`).
- **Must still hold on logs-before:** `residues.twinGroups > 0`. Quote `nearAt`,
  `residueShare`, `twinGroups`, `twinLines`, `modalSplit`, `splitLag`, `stretched`.
- **Must still hold as a Con:** `residues.stretched` is false and `modalSplit=0` — the
  leftover mode is identical copies, not a DNA-tight climb. Use `dups.copyShare` (this
  iteration) rather than treating `stretched` as the go signal.
