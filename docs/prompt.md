# Iteration Prompt

Use this prompt to trigger a single iteration. Paste it as-is into Cursor (or an equivalent
AI-assisted IDE) with this repository open, so the agent has `AGENTS.md` and `docs/tz.md`
available in context.

---

Run the next iteration of the WebJCVI project, following the full iteration
algorithm defined in AGENTS.md and docs/tz.md, in order, without skipping any
step. Write all logs, code comments, commit messages, agent-instruction files,
and any other artifacts in English.

The useful system being grown is **document and log analysis for AI agents**.
The agent must **compare old analysis with new analysis** before writing Pros
and Cons. Do not form a verdict from a single JSON snapshot.

Pay special attention to the following requirements for this iteration:

1. The DNA analysis and report-building algorithm must evolve every
   iteration. Produce a new version of the DNA report builder that is
   meaningfully different from every previous version recorded in
   docs/log.md — not a cosmetic tweak, but a genuinely different way of
   reading the DNA. The resulting report must differ from all prior reports
   in both content and structure, not just in minor details.

2. The web and MCP surfaces must do useful work for **document and log
   analysis by AI agents**, beyond DNA itself. Both surfaces must expose
   functionality that is genuinely useful on its own. DNA-related features
   alone are not sufficient. Keep the previous text-analysis algorithm in
   the facade; add, do not replace.

3. Before forming a new hypothesis, **read the immediately previous
   docs/log.md entry** (hypothesis and its Pros/Cons) so the new theory
   answers those cons. Then read the rest of the log for novelty. If
   `docs/agent/iteration-<previous>.md` exists, read it too.

4. The hypothesis must contain **four explicit parts** (docs/tz.md §6.4):
   DNA/text rule (pointed at a concrete signal from this iteration's DNA
   report); usefulness for documents and logs; practical use (how an agent
   uses it); verification criterion (pass/fail on before vs after JSON for
   logs and for docs/tz.md). Every code change must trace back to that rule.

5. Before any code change, capture the **old** reports through the current
   facade: `reports/logs/logs-before.json` (from test.log) and
   `reports/logs/tz-before.json` (from docs/tz.md). After the code change
   and a green test run, capture the **new** reports:
   `reports/logs/logs-after.json` and `reports/logs/tz-after.json`. All
   four files must exist before Pros/Cons. Also write `report.json` as a
   copy of `logs-after.json` (Gradle analyzeLogs may produce that copy).

6. Besides the code change, write **new** agent instructions at
   `docs/agent/iteration-<index>.md`. Do not edit or delete previous
   instruction files. The new file must include: how to use the
   text-analysis JSON; this iteration's verification criterion; criteria
   for checking the **previous** step.

7. **Forbidden to write Pros/Cons until** the agent has read: the DEBUG
   logs, docs/tz.md, the four JSON reports (comparing old vs new on both
   corpora), old and new agent instructions (with criteria), and the
   four-part hypothesis. Pros/Cons must state whether the verification
   criterion passed, grounded in the four JSON files — not speculation.

Record the iteration in docs/log.md using exactly the required format
(four-part Hypothesis / Functionality changes / Agent instructions / Pros /
Cons / Report builder changes). Commit locally with message
"Iteration #<index> - <brief description>" — do not push.
