# Iteration Prompt

Use this prompt to trigger a single iteration. Paste it as-is into Cursor (or an equivalent
AI-assisted IDE) with this repository open, so the agent has `AGENTS.md` and `docs/tz.md`
available in context.

---

Run the next iteration of the WebJCVI project, following the full iteration
algorithm defined in AGENTS.md and docs/tz.md, in order, without skipping any
step. Write all logs, code comments, commit messages, and any other artifacts
in English.

Pay special attention to the following requirements for this iteration:

1. The DNA analysis and report-building algorithm must evolve every
   iteration. Produce a new version of the DNA report builder that is
   meaningfully different from every previous version recorded in
   docs/log.md — not a cosmetic tweak, but a genuinely different way of
   reading the DNA. The resulting report must differ from all prior reports
   in both content and structure, not just in minor details.

2. The web and MCP surfaces must do useful work beyond DNA itself. Both
   surfaces must expose functionality that is genuinely useful on its own —
   e.g. related to information processing, code analysis, memory/knowledge
   management, or a similar domain — not only functionality for viewing or
   serving DNA reports. DNA-related features alone are not sufficient for
   this iteration.

3. You must construct and justify the link between the DNA and this useful
   system. Explicitly explain why the rule(s) you extracted from the DNA
   this iteration imply the specific useful functionality you are building.
   This justification must be concrete: point to an actual feature of the
   DNA (a motif, a statistical property, a structural pattern, a
   repeat/frequency you found, etc. — surfaced by your report from step 1),
   not a vague or after-the-fact rationalization.

4. Every change must be derived from an extracted rule, never arbitrary or
   chosen by preference. If you cannot trace a piece of functionality back
   to a specific rule you extracted from the DNA this iteration, do not
   build it. Before forming your hypothesis, read the full history in
   docs/log.md and confirm your hypothesis is meaningfully different from
   every previous one.

5. Before Pros/Cons, run the full automated test suite so DEBUG logs are
   written to reports/logs/test.log. Then call the log-analysis facade on
   that journal and write reports/logs/report.json (Gradle analyzeLogs).
   Do not skip this even if the journal looks empty.

6. Read reports/logs/report.json before writing Pros and Cons. Any claim
   about whether the hypothesis is useful for log analysis must be grounded
   in that JSON (what the algorithms actually found in the app's own logs),
   not in speculation. Record that grounding in the Pros/Cons fields.

Record the iteration in docs/log.md using exactly the required format
(Hypothesis / Functionality changes / Pros / Cons / Report builder changes),
making sure the "Pros"/"Cons" and "Hypothesis" fields carry the DNA-to-
functionality justification from point 3 above. Commit locally with message
"Iteration #<index> - <brief description>" — do not push.