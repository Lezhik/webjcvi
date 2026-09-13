# Agent instructions — iteration 24

How to use the text-analysis JSON for documents and logs. English. Do not treat this
file as a DNA decoder. Read this file **before** treating `analyze_logs` (or
`profile_runways`) output as a verdict.

Previous instruction file: `docs/agent/iteration-23.md`.

## How to use this report

Call `analyze_logs` on caller text (a log or a document). The JSON is an accumulating
toolkit. New in this iteration is **`runways`**:

| Key | Meaning for an agent |
| --- | --- |
| `forkAt` | First difference inside the largest colliding 16-prefix (same as `cliffs.forkAt`). |
| `cliffAt` | Smallest prefix k with unique share ≥ 0.99 (same as `cliffs.cliffAt`). |
| `runway` | `cliffAt − forkAt` when both exist and the cliff is past the fork; else 0. Layout body to strip. |
| `stretched` | True when `runway > 20` (DNA uniqueAt). Then start an identifier at `cliffAt`, not `forkAt`. |
| `shareAt16` / `topPrefix` | Clock veto and colliding prefix from iteration 22–23. |

If `cliffs.forkAt` is only barely past 16, **do not** slice records from `forkAt` —
read `runways.stretched`. If stretched, the payload starts at `cliffAt`; `[forkAt, cliffAt)`
is logger/thread/class layout.

MCP follow-up: `profile_runways`. `profile_cliffs` remains the fork/cliff pair.

On documents, `runway=0` means uniqueness cliffs at or before the first boilerplate split
(identical fences never fork).

## Verification criterion (this iteration)

**Pass if all of:**

1. `runways` is **absent** from `reports/logs/logs-before.json` and
   `reports/logs/tz-before.json`.
2. `runways` is **present** in `reports/logs/logs-after.json` and
   `reports/logs/tz-after.json`, with `tileLength=16`.
3. On **logs-after**, `runways.runway > 20` and `runways.stretched` is true — the
   identifier is not DNA-tight.

**Fail if** logs-after `runway ≤ 20` or `runway=0` (still treating forkAt as the id),
even when the `runways` section exists. Quote `forkAt`, `cliffAt`, `runway`, and
`stretched` from both after files when scoring.

## Check the previous step (iteration 23)

Re-check iteration 23 against **logs-before** (old facade, has `cliffs`, no `runways`):

- Hypothesis claimed the first split sits past the 16-character clock.
- **Must still hold on logs-before:** `cliffs.forkAt > 16` and `cliffs.tileLength=16`.
  Quote `forkAt`, `cliffAt`, and `shareAt16`.
- `cliffAt` should still be far from DNA uniqueAt=20 — the void that iteration 23
  recorded as a Con (forkAt is only seconds, not the identifier).
- Do **not** treat `forkAt > 16` as “start the id here.” Use `runways.stretched`
  (this iteration) rather than repeating a clock-cliff start column.
