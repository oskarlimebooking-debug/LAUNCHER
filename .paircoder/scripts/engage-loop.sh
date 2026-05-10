#!/usr/bin/env bash
# engage-loop.sh — drive bpsai-pair engage --resume in a loop, working around
# the post-task meaningful-output heuristic that overwrites successful task
# status from `done` back to `failed` in the working tree (committed status
# is fine — only the working tree is corrupted).
#
# Each iteration:
#   1. Revert any working-tree task-file edits to HEAD (restores `done` status
#      that engage committed but then clobbered).
#   2. Run `bpsai-pair engage <backlog> --resume`.
#   3. Inspect the run output:
#        - If "Sprint complete" with 0 failures and 0 work attempted → exit 0
#        - If a real failure (no commit produced for the attempted task) →
#          exit non-zero so a human can investigate.
#        - Otherwise → loop.
#
# Usage: engage-loop.sh <backlog.md> [max-iterations]
set -euo pipefail

BACKLOG="${1:?usage: engage-loop.sh <backlog.md> [max-iterations]}"
MAX_ITER="${2:-50}"

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

iter=0
while (( iter < MAX_ITER )); do
  iter=$(( iter + 1 ))
  echo ""
  echo "=== engage-loop iteration $iter ==="

  # Step 1 — revert spurious working-tree status flips.
  if git status --short -- .paircoder/tasks/ | grep -q .; then
    echo "→ reverting working-tree task-file edits to HEAD"
    git checkout HEAD -- .paircoder/tasks/
  fi

  # Step 2 — capture HEAD before, run engage.
  head_before=$(git rev-parse HEAD)
  out=$(mktemp)
  trap 'rm -f "$out"' EXIT
  if ! bpsai-pair engage "$BACKLOG" --resume --hooks-advisory --skip-planning 2>&1 | tee "$out"; then
    echo "✗ engage exited non-zero on iteration $iter"
    exit 1
  fi
  head_after=$(git rev-parse HEAD)

  # Step 3 — decide whether to continue.
  if grep -qE "Skipping [0-9]+ completed, running 0 remaining" "$out"; then
    echo "✓ all tasks completed"
    exit 0
  fi
  if [[ "$head_before" == "$head_after" ]]; then
    echo "✗ engage iteration $iter produced no commits — likely a real failure"
    echo "  inspect: $out"
    tail -40 "$out"
    exit 2
  fi
  # Count work-commits this iteration to gauge progress.
  commits_this_iter=$(git log --oneline "$head_before..$head_after" | wc -l | tr -d ' ')
  echo "→ iteration $iter shipped $commits_this_iter commits; continuing"
done

echo "✗ hit max iterations ($MAX_ITER) without sprint completion"
exit 3
