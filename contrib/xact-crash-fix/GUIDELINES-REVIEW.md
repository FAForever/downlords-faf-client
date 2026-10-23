# Compliance review against FAForever/java-guidelines wiki

Source: `github.com/FAForever/java-guidelines/wiki` — 3 pages total: **Home** (just points at
the other two), **Coding Standards**, **Contribution Guidelines**. Full relevant content below,
evaluated against our actual `contrib/xact-crash-fix/src/*.c` files and the PR/commit already made.

## What the guidelines actually say

### Coding Standards (thin — mostly Java-specific, noted where N/A)
- Never throw generic `Error`/`RuntimeException`/`Throwable`/`Exception`. **N/A** — no Java exceptions in C.
- "Exception handlers should preserve the original exception." **Loosely applies**: we don't swallow
  failures silently — every `MH_CreateHook`/`MH_EnableHook`/`AddVectoredExceptionHandler` failure gets
  logged with its actual status code, not discarded.
- SLF4J logging, placeholder-quoting style (`'{}'` not bare `{}`). **N/A**, Java-specific API — but the
  underlying principle (don't use raw stdout, make logged values visually distinct) is honored: we use
  `OutputDebugStringA` (the correct native equivalent of a debug channel, not `printf`/console output
  in the shipped DLL — `printf` only appears in `test_harness.c`, a standalone dev tool, not the DLL
  itself), and log lines already read like `"...target=%p, strategy=%s) -> %d"` with clear separation.

### Contribution Guidelines (the real substance)

> "Quality has highest priority" · "'Quick and dirty' implementations are discouraged... there is only
> clean and solid" · "Prefer longer, unambiguous names over short but ambiguous ones" · "Prefer slower
> but easily readable code over short/fast but hard to read code" · "Even small things matter"

> Comments/JavaDoc: "comments are bad practice"... but "know when to ignore this rule."

> TODO/FIXME: `FIXME` = wrong/missing code, `TODO` = works but needs improvement. Always mark
> temporary/unfinished work with one of these plus an explanation.

> Process: issue on GitHub first → branch `feature/#00-some-keywords` (`00` = issue ID) → commit
> messages include the issue ID, e.g. `#23 implemented update task`.

> Definition of Done: "approach 100% test coverage (line & branch)" · reviewed by another contributor ·
> ready to merge into `develop`.

**No section anywhere addresses non-Java, native, or "contrib"-style contributions.** No guidance
either way — this repo's Java-centric process wasn't written with a case like ours in mind.

## Compliance checklist

| Guideline | Status | Notes |
|---|---|---|
| Unambiguous naming | ⚠️ **Partial** | See below — the offset-encoded names (`Detour_0041d751` etc.) are the clearest violation. Flagged for the sibling renaming pass. |
| Comments: "avoid, but know when to ignore" | ✅ **Compliant, deliberately** | Every comment in this codebase explains *why* (reverse-engineered behavior, an unusual C-can't-do-`__try__except` workaround), never *what* — exactly the case the guideline's own escape hatch describes. Worth stating this explicitly if a reviewer questions the comment density; it's not an oversight. |
| TODO/FIXME usage | ✅ **Compliant** | None used, and correctly so — nothing here is unfinished/broken code. What's unverified (e.g. "doesn't exercise the actual recovery path", DLL-version portability) is disclosed in prose in the README/INTEGRATION docs as a *scope limitation*, not left as an in-code TODO masking a real gap. |
| No raw stdout in shipped code | ✅ **Compliant** | `OutputDebugStringA` in the DLL; `printf` only in the standalone `test_harness.c` dev tool, which is appropriate there. |
| Preserve/log failures, don't swallow | ✅ **Compliant** | Every hook-install and VEH-registration failure path logs the real status/error code. |
| Issue created before work | ❌ **Not done** | No GitHub issue exists for this. |
| Branch name `feature/#<id>-keywords` | ❌ **Not compliant** | Actual: `bugfix/fix-sound-issue-xactengine2_9-catch-wave-bank-invalid-memory-access` — wrong prefix, no issue ID, and it's a URL-unfriendly, very long slug. |
| Commit message includes issue ID | ❌ **Not done** | No issue ID exists to include. |
| ~100% test coverage | ⚠️ **Honest partial, already disclosed** | `test_harness.c` validates the injection/hook-install mechanics against the real DLL; it explicitly does NOT exercise the setjmp/longjmp recovery path (needs a live game session with real XACT vtables) — this is already stated plainly in the harness's own header comment and in the PR description, not hidden. |
| Reviewed by another contributor | N/A yet | Correctly still in draft. |

## Recommendations, prioritized

1. **Naming pass** (see sibling agent's work) — rename `Detour_0041d751`/`Detour_0041d212`/
   `Detour_0041de46` and their `fpOrig_*`/`Fn_*` typedefs to something that says what each site *does*
   (teardown/UAF, null-hop cue-lookup, list-walk) instead of the raw hex offset. This is the single
   most visible naming gap and the easiest to fix without losing any information — the exact offset is
   still documented in the header comment right above each one.

2. **The issue-first gap is real and worth addressing directly, not worked around.** My recommendation:
   don't rename the branch or rewrite the existing commit to retroactively fake compliance — that
   loses honest history for no real benefit. Instead: **open a GitHub issue now** describing the bug
   (can reuse most of the PR description), then **edit the PR description** to link it (`Fixes #<id>`
   or just `Relates to #<id>`, since this is explicitly a draft/discussion-starter, not a claimed fix
   in the "ready to merge" sense their process implies). That satisfies the spirit (a durable, linkable
   issue exists) without pretending the branch was planned that way from the start — which it wasn't;
   this began as live crash investigation, not a scheduled feature. Worth saying so plainly in a PR
   comment: the process here was necessarily retroactive because the work started as diagnosis, not
   planned development.

3. **Explicitly acknowledge the "avoid comments" tension in the PR description**, briefly — a
   maintainer skimming this code will immediately notice it's far more heavily commented than typical
   FAF Java code. One sentence pointing at the guideline's own "know when to ignore this rule" clause
   heads off that friction before it becomes a review comment.

4. Nothing here blocks the PR staying open as a draft — none of these are "must fix before anyone
   looks at it," they're what to clean up before asking for it to come out of draft.
