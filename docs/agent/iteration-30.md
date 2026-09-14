# Agent instructions — iteration 30

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_lingers`) output as a verdict.

Previous instruction file: `docs/agent/iteration-29.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`lingers`**:

| Key | Meaning for an agent |
| --- | --- |
| `nearAt` / `twinGroups` | Near-unique leftover from iterations 27–29. |
| `lag0` / `lag1` / `lag2` / `lagLong` | Leftover twin groups binned by splitLag = forkAt − nearAt (0 if identical copies). |
| `modalLag` | Most common leftover lag. 0 means copies dominate as groups. |
| `maxLag` | Longest leftover splitLag. |
| `longShare` | `lagLong / twinGroups`. DNA leftover tail is 33/324 ≈ 0.10. |
| `longTail` | True when `lagLong > 0` — some leftover persists past DNA's two-base climb even if the mode is copies or lag 1. |
| `copyShare` / `mostlyCopies` | Copy majority from iteration 29 — still the dedupe class, not the whole leftover. |

If `dups.mostlyCopies` is true, **do not** treat leftover as “dedupe only” when
`lingers.longTail` is true — a minority of leftover groups still share more than two
characters past `nearAt`. Inverse of residue-scan: there the *modal* split had to
stretch; here any lag\>2 group is the tail. Do **not** wait for `cliffAt` (0.99).

MCP follow-up: `profile_lingers`. `profile_dups` remains the copy/fork split.

On documents, `longTail=false` with `modalLag=1` or `lag2` dominating means leftover
forks are DNA-tight (headings that split immediately). Identical fences sit in `lag0`.

## Verification criterion (this iteration)

**Pass if all of:**

1. `lingers` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `lingers` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `floorLength=8`.
3. On **logs-after**, `lingers.lagLong > 0` and `lingers.longTail` is true — leftover
   after 0.90 includes groups that persist past DNA's two-base climb, even when
   leftover copies dominate.

**Fail if** logs-after `lagLong=0` or `longTail` is false, even when the `lingers`
section exists. Quote `nearAt`, `twinGroups`, `lag0`, `lag1`, `lag2`, `lagLong`,
`modalLag`, `maxLag`, `longShare`, `longTail`, and `mostlyCopies` from both after
files when scoring.

## Check the previous step (iteration 29)

Re-check iteration 29 against **logs-before** (old facade, has `dups`, no `lingers`):

- Hypothesis claimed leftover after 0.90 includes exact copies (`copyGroups > 0` and
  `copyShare > 0`), unlike DNA copyShare=0.
- **Must still hold on logs-before:** `dups.copyGroups > 0` and `dups.copyShare > 0`.
  Quote `nearAt`, `copyGroups`, `copyLines`, `forkGroups`, `forkLines`, `copyShare`,
  `mostlyCopies`, `minFork`.
- **Must still hold as a Con:** leftover *forks* look DNA-tight (`minFork` is only one
  past `nearAt`) even when `mostlyCopies` is true. Use `lingers.longTail` (this
  iteration) rather than treating leftover forks as all lag 1, and rather than
  treating copy-majority as the whole leftover.
