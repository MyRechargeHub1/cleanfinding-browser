#!/bin/bash
# Smart Monitor - Automated Error Detection & Improvement System
# Scans worker code, config, security, and performance
# Creates GitHub issues for findings
set -euo pipefail

ISSUES_FILE="/tmp/monitor-issues.json"
REPORT_FILE="/tmp/monitor-report.md"
WORKER_FILE="_worker.js"
CONFIG_FILE="wrangler.toml"
JEKYLL_CONFIG="_config.yml"
ISSUE_COUNT=0
WARNING_COUNT=0
INFO_COUNT=0

# Colors for terminal
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

> "$ISSUES_FILE"
> "$REPORT_FILE"

echo -e "${BLUE}=== Smart Monitor v1.0 ===${NC}"
echo -e "${BLUE}Repository: ${REPO_NAME:-unknown}${NC}"
echo -e "${BLUE}Type: ${REPO_TYPE:-unknown}${NC}"
echo ""

# Helper: add an issue to the queue
add_issue() {
  local severity="$1"  # critical, warning, info, feature
  local title="$2"
  local body="$3"
  local labels="${4:-smart-monitor}"

  local emoji="ℹ️"
  case "$severity" in
    critical) emoji="🔴"; ((ISSUE_COUNT++)) || true ;;
    warning)  emoji="🟡"; ((WARNING_COUNT++)) || true ;;
    info)     emoji="🔵"; ((INFO_COUNT++)) || true ;;
    feature)  emoji="🟢"; ((INFO_COUNT++)) || true ;;
  esac

  echo -e "${emoji} [$severity] $title"

  # Write to issues file
  jq -n \
    --arg title "[$severity] $title" \
    --arg body "$body" \
    --arg labels "$labels" \
    --arg severity "$severity" \
    '{title: $title, body: $body, labels: $labels, severity: $severity}' >> "$ISSUES_FILE"

  # Write to report
  echo "### $emoji $title" >> "$REPORT_FILE"
  echo "$body" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
}

# ========================================
# 1. WORKER CODE QUALITY CHECKS
# ========================================
echo -e "${BLUE}--- Worker Code Quality ---${NC}"

if [ -f "$WORKER_FILE" ]; then
  WORKER_SIZE=$(wc -c < "$WORKER_FILE")
  WORKER_LINES=$(wc -l < "$WORKER_FILE")
  echo "  Worker: ${WORKER_LINES} lines, ${WORKER_SIZE} bytes"

  # Check: Multiple indexes in writeDataPoint (CRITICAL - causes silent data loss)
  multi_idx=$(grep -n 'indexes:.*\[.*,.*\]' "$WORKER_FILE" 2>/dev/null || true)
  if [ -n "$multi_idx" ]; then
    add_issue "critical" "writeDataPoint uses multiple indexes - causes silent data loss" \
"Cloudflare Analytics Engine \`writeDataPoint()\` only supports **1 index**. Using multiple indexes causes data to be silently discarded (no error thrown, no data written).

**Found at:**
\`\`\`
$multi_idx
\`\`\`

**Fix:** Use only 1 element in the indexes array:
\`\`\`javascript
indexes: [singleValue]  // NOT [value1, value2]
\`\`\`

Move extra values to the \`blobs\` array instead." \
"smart-monitor,bug,critical"
  fi

  # Check: toDateTime() usage (not supported by Analytics Engine SQL)
  todatetime=$(grep -n 'toDateTime(' "$WORKER_FILE" 2>/dev/null || true)
  if [ -n "$todatetime" ]; then
    add_issue "warning" "toDateTime() used in Analytics Engine queries - not supported" \
"Cloudflare Analytics Engine SQL does not support \`toDateTime()\` with string arguments. This causes custom date range queries to fail silently.

**Found at:**
\`\`\`
$todatetime
\`\`\`

**Fix:** Use direct string comparison:
\`\`\`sql
-- Instead of: col >= toDateTime('2026-01-01 00:00:00')
-- Use:        col >= '2026-01-01 00:00:00'
\`\`\`" \
"smart-monitor,bug"
  fi

  # Check: INTERVAL syntax (common mistake)
  bad_interval=$(grep -n "INTERVAL '[0-9]* [A-Z]" "$WORKER_FILE" 2>/dev/null || true)
  if [ -n "$bad_interval" ]; then
    add_issue "warning" "Wrong INTERVAL syntax in Analytics Engine query" \
"Analytics Engine SQL requires number and unit as **separate** quoted values.

**Found at:**
\`\`\`
$bad_interval
\`\`\`

**Fix:**
\`\`\`sql
-- Wrong: INTERVAL '7 DAY'
-- Right: INTERVAL '7' DAY
\`\`\`" \
"smart-monitor,bug"
  fi

  # Check: Blob position consistency
  # Extract all writeDataPoint calls and check blob order
  blob_calls=$(grep -n 'blobs:' "$WORKER_FILE" 2>/dev/null | head -10 || true)
  if [ -n "$blob_calls" ]; then
    # Check if blob positions are documented
    has_blob_comments=$(grep -c '// blob[0-9]' "$WORKER_FILE" 2>/dev/null || echo "0")
    if [ "$has_blob_comments" -lt 2 ]; then
      add_issue "info" "Blob positions lack inline documentation" \
"Analytics Engine blob positions should be documented inline for maintainability. Without clear comments, blob position mismatches are common and hard to debug.

**Suggestion:** Add position comments to every \`writeDataPoint\` call:
\`\`\`javascript
blobs: [
  url.pathname,    // blob1: page path
  country,         // blob2: country code
  device,          // blob3: device type
  'pageview',      // blob4: event type
  cacheStatus,     // blob5: cache status
]
\`\`\`" \
"smart-monitor,enhancement"
    fi
  fi

  # Check: Error handling in fetch handler
  has_try_catch=$(grep -c 'try {' "$WORKER_FILE" 2>/dev/null || echo "0")
  if [ "$has_try_catch" -lt 1 ]; then
    add_issue "warning" "Worker fetch handler lacks error handling" \
"The main \`fetch\` handler should wrap asset serving in try/catch to prevent unhandled errors from breaking the site.

**Suggestion:** Add try/catch around \`env.ASSETS.fetch()\` calls." \
"smart-monitor,bug"
  fi

  # Check: ctx.waitUntil for analytics (should not await)
  await_analytics=$(grep -n 'await.*trackAnalytics\|await.*writeDataPoint' "$WORKER_FILE" 2>/dev/null || true)
  if [ -n "$await_analytics" ]; then
    add_issue "warning" "Analytics tracking should use ctx.waitUntil, not await" \
"Awaiting analytics calls blocks the response. Use \`ctx.waitUntil()\` for fire-and-forget tracking.

**Found at:**
\`\`\`
$await_analytics
\`\`\`

**Fix:**
\`\`\`javascript
ctx.waitUntil(trackAnalytics(request, response, env.ANALYTICS, startTime));
\`\`\`" \
"smart-monitor,performance"
  fi

  # Check: Worker size warning
  if [ "$WORKER_SIZE" -gt 1048576 ]; then
    add_issue "warning" "Worker script exceeds 1MB ($((WORKER_SIZE/1024))KB)" \
"Cloudflare Workers have a 10MB limit for paid plans and 1MB for free. Large workers have slower cold starts.

**Current size:** $((WORKER_SIZE/1024))KB / ${WORKER_LINES} lines

**Suggestions:**
- Move large data to KV or R2
- Use dynamic imports for rarely-used code
- Minify the worker for production" \
"smart-monitor,performance"
  fi

  # Check: Hardcoded secrets
  secrets_found=$(grep -inE '(api[_-]?key|api[_-]?token|secret|password|credential)\s*[:=]\s*["\x27][A-Za-z0-9]' "$WORKER_FILE" 2>/dev/null | grep -v 'env\.' | grep -v '//' || true)
  if [ -n "$secrets_found" ]; then
    add_issue "critical" "Potential hardcoded secrets in worker code" \
"Found possible hardcoded API keys or tokens. These should be stored as Cloudflare Worker secrets.

**Found at:**
\`\`\`
$secrets_found
\`\`\`

**Fix:** Use environment variables via \`wrangler secret put\` or Cloudflare Dashboard." \
"smart-monitor,security,critical"
  fi

else
  echo "  No _worker.js found"
fi

# ========================================
# 2. CONFIGURATION CHECKS
# ========================================
echo -e "${BLUE}--- Configuration Checks ---${NC}"

if [ -f "$CONFIG_FILE" ]; then
  # Check: Analytics binding
  has_analytics=$(grep -c 'ANALYTICS' "$CONFIG_FILE" 2>/dev/null || echo "0")
  if [ "$has_analytics" -lt 1 ]; then
    add_issue "info" "No Analytics Engine binding configured" \
"The \`wrangler.toml\` does not have an Analytics Engine binding. To enable server-side analytics:

\`\`\`toml
[[analytics_engine_datasets]]
binding = \"ANALYTICS\"
dataset = \"your_dataset_name\"
\`\`\`" \
"smart-monitor,enhancement"
  fi

  # Check: Compatibility date
  compat_date=$(grep 'compatibility_date' "$CONFIG_FILE" 2>/dev/null | head -1 || true)
  if [ -n "$compat_date" ]; then
    date_val=$(echo "$compat_date" | grep -oP '\d{4}-\d{2}-\d{2}' || true)
    if [ -n "$date_val" ]; then
      date_epoch=$(date -d "$date_val" +%s 2>/dev/null || echo "0")
      six_months_ago=$(date -d "6 months ago" +%s 2>/dev/null || echo "0")
      if [ "$date_epoch" -lt "$six_months_ago" ] && [ "$date_epoch" -gt 0 ]; then
        add_issue "info" "Compatibility date is older than 6 months ($date_val)" \
"Consider updating \`compatibility_date\` in wrangler.toml to access newer Workers features and fixes.

**Current:** \`$date_val\`
**Suggestion:** Update to a recent date to get bug fixes and new features." \
"smart-monitor,enhancement"
      fi
    fi
  fi
else
  echo "  No wrangler.toml found"
fi

# Jekyll config checks
if [ -f "$JEKYLL_CONFIG" ]; then
  # Check: _worker.js in include list (causes deploy failure)
  worker_in_include=$(grep -A 20 '^include:' "$JEKYLL_CONFIG" 2>/dev/null | grep '_worker.js' || true)
  if [ -n "$worker_in_include" ]; then
    add_issue "critical" "_worker.js in Jekyll include list - breaks wrangler deploy" \
"Having \`_worker.js\` in the Jekyll \`include:\` list causes it to be copied into \`_site/\`. Wrangler 4.x refuses to upload it as a static asset with error:
\`\`\`
Uploading a Pages _worker.js file as an asset.
This could expose your private server-side code to the public Internet.
\`\`\`

**Fix:** Remove \`_worker.js\` from the \`include:\` list in \`_config.yml\`. The worker is deployed separately via \`wrangler.toml\` (\`main = \"_worker.js\"\`)." \
"smart-monitor,bug,critical"
  fi

  # Check: _routes.json in include list (not needed for Workers)
  routes_in_include=$(grep -A 20 '^include:' "$JEKYLL_CONFIG" 2>/dev/null | grep '_routes.json' || true)
  if [ -n "$routes_in_include" ]; then
    add_issue "warning" "_routes.json in Jekyll include - not needed for Workers deployment" \
"\`_routes.json\` is a Cloudflare Pages feature. Workers with \`[assets] binding = \"ASSETS\"\` route ALL requests through the worker. Remove from include list and delete the file." \
"smart-monitor,bug"
  fi
fi

# ========================================
# 3. SECURITY CHECKS
# ========================================
echo -e "${BLUE}--- Security Scan ---${NC}"

# Check: .env files committed
env_files=$(find . -name '.env' -o -name '.env.local' -o -name '.env.production' 2>/dev/null | grep -v node_modules | grep -v .git || true)
if [ -n "$env_files" ]; then
  add_issue "critical" ".env file committed to repository" \
"Found environment files that may contain secrets:
\`\`\`
$env_files
\`\`\`

**Fix:**
1. Add to \`.gitignore\`: \`.env*\`
2. Remove from git: \`git rm --cached .env\`
3. Rotate any exposed credentials immediately" \
"smart-monitor,security,critical"
fi

# Check: Credentials in config files
creds_in_config=$(grep -rnE '(ghp_|sk-|AKIA|AIza|token.*=.*[A-Za-z0-9]{20,})' . --include='*.yml' --include='*.yaml' --include='*.toml' --include='*.json' 2>/dev/null | grep -v node_modules | grep -v '.git/' | grep -v 'secrets\.' | head -5 || true)
if [ -n "$creds_in_config" ]; then
  add_issue "critical" "Potential credentials found in config files" \
"Found patterns matching API tokens or credentials:
\`\`\`
$creds_in_config
\`\`\`

**Action:** Rotate these credentials immediately and use GitHub Secrets instead." \
"smart-monitor,security,critical"
fi

# Check: SQL injection potential in analytics queries
if [ -f "$WORKER_FILE" ]; then
  unsafe_sql=$(grep -n 'runQuery.*\$\{.*searchParams\|runQuery.*\$\{.*body\.' "$WORKER_FILE" 2>/dev/null || true)
  if [ -n "$unsafe_sql" ]; then
    add_issue "warning" "Potential SQL injection in analytics queries" \
"User input may be passed directly into SQL queries without sanitization.

**Found at:**
\`\`\`
$unsafe_sql
\`\`\`

**Fix:** Validate and sanitize all user inputs before including in SQL strings. Use allowlists for column names and validate date formats." \
"smart-monitor,security"
  fi
fi

# ========================================
# 4. PERFORMANCE CHECKS
# ========================================
echo -e "${BLUE}--- Performance Analysis ---${NC}"

if [ -f "$WORKER_FILE" ]; then
  # Count sequential API calls in analytics
  seq_queries=$(grep -c 'await runQuery' "$WORKER_FILE" 2>/dev/null || echo "0")
  parallel_queries=$(grep -c 'Promise.all' "$WORKER_FILE" 2>/dev/null || echo "0")

  if [ "$seq_queries" -gt 5 ] && [ "$parallel_queries" -lt 1 ]; then
    add_issue "warning" "Analytics queries are sequential - parallelize with Promise.all" \
"Found $seq_queries sequential \`await runQuery()\` calls but no \`Promise.all()\`. Each sequential call adds ~50-200ms latency.

**Suggestion:** Group independent queries into \`Promise.all()\`:
\`\`\`javascript
const [pageViews, countries, devices] = await Promise.all([
  runQuery('SELECT ... page views'),
  runQuery('SELECT ... countries'),
  runQuery('SELECT ... devices'),
]);
\`\`\`" \
"smart-monitor,performance"
  fi

  # Check for missing cache headers
  cache_headers=$(grep -c 'Cache-Control' "$WORKER_FILE" 2>/dev/null || echo "0")
  if [ "$cache_headers" -lt 1 ]; then
    add_issue "info" "No Cache-Control headers set in worker responses" \
"Consider adding cache headers to static responses to improve performance:
\`\`\`javascript
headers.set('Cache-Control', 'public, max-age=3600');
\`\`\`" \
"smart-monitor,performance"
  fi
fi

# ========================================
# 5. BUILD & DEPLOYMENT CHECKS
# ========================================
echo -e "${BLUE}--- Build & Deploy Checks ---${NC}"

# Check: Required files
for file in _worker.js wrangler.toml; do
  if [ ! -f "$file" ]; then
    add_issue "critical" "Missing required file: $file" \
"The file \`$file\` is required for Cloudflare Workers deployment but was not found." \
"smart-monitor,bug,critical"
  fi
done

# Check: GitHub Actions workflow exists
if [ ! -d ".github/workflows" ]; then
  add_issue "warning" "No GitHub Actions workflows configured" \
"No CI/CD pipeline found. Consider adding automated deployment and testing." \
"smart-monitor,enhancement"
else
  deploy_workflow=$(find .github/workflows -name '*.yml' -exec grep -l 'wrangler\|deploy' {} \; 2>/dev/null | head -1 || true)
  if [ -z "$deploy_workflow" ]; then
    add_issue "warning" "No deployment workflow found in GitHub Actions" \
"GitHub Actions workflows exist but none appear to handle deployment. Add a workflow with \`wrangler deploy\` command." \
"smart-monitor,enhancement"
  fi
fi

# Check: .gitignore exists and covers common files
if [ -f ".gitignore" ]; then
  for pattern in node_modules .env _site; do
    if ! grep -q "$pattern" .gitignore 2>/dev/null; then
      add_issue "info" ".gitignore missing pattern: $pattern" \
"Add \`$pattern\` to \`.gitignore\` to prevent accidental commits." \
"smart-monitor,enhancement"
    fi
  done
fi

# ========================================
# 6. FEATURE SUGGESTIONS
# ========================================
echo -e "${BLUE}--- Feature Suggestions ---${NC}"

if [ -f "$WORKER_FILE" ]; then
  # Suggest: Rate limiting
  has_rate_limit=$(grep -c 'rateLimit\|rate.limit\|rate_limit\|throttle' "$WORKER_FILE" 2>/dev/null || echo "0")
  if [ "$has_rate_limit" -lt 1 ]; then
    add_issue "feature" "Add rate limiting to API endpoints" \
"No rate limiting detected. Consider adding rate limiting to protect API endpoints from abuse:

\`\`\`javascript
// Simple in-memory rate limiter
const rateLimits = new Map();
function checkRateLimit(ip, limit = 60, window = 60000) {
  const now = Date.now();
  const key = ip;
  const record = rateLimits.get(key) || { count: 0, reset: now + window };
  if (now > record.reset) { record.count = 0; record.reset = now + window; }
  record.count++;
  rateLimits.set(key, record);
  return record.count <= limit;
}
\`\`\`" \
"smart-monitor,enhancement,feature"
  fi

  # Suggest: Bot filtering in analytics
  has_bot_filter=$(grep -c 'bot\|crawler\|spider\|isBot' "$WORKER_FILE" 2>/dev/null || echo "0")
  if [ "$has_bot_filter" -lt 1 ]; then
    add_issue "feature" "Add bot traffic filtering to analytics" \
"Analytics may be polluted by bot traffic (WordPress scanners, crawlers). Consider filtering known bots:

\`\`\`javascript
function isBot(userAgent) {
  const botPatterns = /bot|crawler|spider|headless|phantom|selenium|wget|curl|python|go-http|scan/i;
  return botPatterns.test(userAgent);
}

// In trackAnalytics:
if (isBot(userAgent)) {
  // Either skip tracking or mark as bot
  blobs.push('bot');
}
\`\`\`

This would clean up your analytics dashboards significantly." \
"smart-monitor,enhancement,feature"
  fi

  # Suggest: Health check endpoint
  has_health=$(grep -c '/health\|/api/health\|/api/status' "$WORKER_FILE" 2>/dev/null || echo "0")
  if [ "$has_health" -lt 1 ]; then
    add_issue "feature" "Add /api/health endpoint for uptime monitoring" \
"A health check endpoint enables external monitoring services (UptimeRobot, Pingdom) to verify your site is running:

\`\`\`javascript
if (url.pathname === '/api/health') {
  return new Response(JSON.stringify({
    status: 'ok',
    timestamp: new Date().toISOString(),
    bindings: {
      analytics: !!env.ANALYTICS,
      ai: !!(env.AI || env.a1),
      assets: !!env.ASSETS,
    }
  }), { headers: { 'Content-Type': 'application/json' } });
}
\`\`\`" \
"smart-monitor,enhancement,feature"
  fi
fi

# ========================================
# 7. RECENT CHANGES ANALYSIS
# ========================================
echo -e "${BLUE}--- Recent Changes ---${NC}"

# Check for large recent commits
large_commits=$(git log --oneline --diff-filter=AM --since="7 days ago" --stat 2>/dev/null | grep -E '\d+ insertion' | awk '{sum+=$4} END {print sum+0}' || echo "0")
if [ "$large_commits" -gt 500 ]; then
  echo "  Large changes in past week: ${large_commits} insertions"
fi

# Check for reverted commits (sign of instability)
reverts=$(git log --oneline --since="30 days ago" 2>/dev/null | grep -ci 'revert' || echo "0")
if [ "$reverts" -gt 2 ]; then
  add_issue "info" "Multiple reverts detected in last 30 days ($reverts)" \
"Found $reverts revert commits in the last 30 days. This may indicate instability. Consider:
- More thorough testing before merging
- Using feature branches with PR reviews
- Adding automated tests" \
"smart-monitor,process"
fi

# ========================================
# SUMMARY
# ========================================
echo ""
echo -e "${BLUE}=== Scan Complete ===${NC}"
TOTAL=$((ISSUE_COUNT + WARNING_COUNT + INFO_COUNT))
echo -e "  ${RED}Critical: $ISSUE_COUNT${NC}"
echo -e "  ${YELLOW}Warnings: $WARNING_COUNT${NC}"
echo -e "  ${GREEN}Info/Features: $INFO_COUNT${NC}"
echo -e "  Total findings: $TOTAL"

# Write summary to report
cat >> "$REPORT_FILE" << EOF

---
## Summary
- **Critical Issues:** $ISSUE_COUNT
- **Warnings:** $WARNING_COUNT
- **Info/Features:** $INFO_COUNT
- **Total:** $TOTAL

*Scanned on $(date -u '+%Y-%m-%d %H:%M UTC') by Smart Monitor v1.0*
EOF

if [ "$ISSUE_COUNT" -gt 0 ]; then
  echo -e "${RED}Action required: $ISSUE_COUNT critical issues found!${NC}"
  exit 1
fi
