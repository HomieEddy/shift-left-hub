#!/usr/bin/env bash
# Open a PR for the current branch and enable auto-merge (squash).
# Usage: scripts/auto-merge-pr.sh <pr-body-file> <pr-title>
set -euo pipefail

BODY_FILE="${1:?path to PR body file required}"
TITLE="${2:?PR title required}"

BRANCH=$(git rev-parse --abbrev-ref HEAD)

git push -u origin "$BRANCH" >/dev/null

PR_URL=$(gh pr create \
  --base master \
  --head "$BRANCH" \
  --title "$TITLE" \
  --body-file "$BODY_FILE")
PR_NUMBER=$(echo "$PR_URL" | grep -oE '[0-9]+$' | head -1)

gh pr merge --auto --squash --delete-branch "$PR_NUMBER" >/dev/null

echo "$PR_URL"
