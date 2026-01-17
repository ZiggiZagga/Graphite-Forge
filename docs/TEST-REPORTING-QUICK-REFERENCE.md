# Test Reporting System - Quick Reference Guide

## 30-Second Quick Start

```bash
cd /workspaces/Graphite-Forge
bash scripts/comprehensive-test-reporter.sh --all
cat test-results/reports/LATEST-SUMMARY.md
cat test-results/reports/test-report-*-todos.md
```

## One-Liners

```bash
# Run all tests with reports
bash scripts/comprehensive-test-reporter.sh --all

# Run only backend tests
bash scripts/comprehensive-test-reporter.sh --backend

# Run only E2E tests
bash scripts/comprehensive-test-reporter.sh --e2e

# Run only roadmap tests
bash scripts/comprehensive-test-reporter.sh --roadmap

# Run with verbose output
bash scripts/comprehensive-test-reporter.sh --all --verbose

# Show help
bash scripts/comprehensive-test-reporter.sh --help
```

## Reports Generated

After running, you'll find:

1. **LATEST-SUMMARY.md** - Quick overview with pass rate
2. **test-report-TIMESTAMP.md** - Full report
3. **test-report-TIMESTAMP-todos.md** - **Action items!**
4. **test-report-TIMESTAMP.json** - For tools/integrations
5. **test-report-TIMESTAMP.log** files - Detailed logs

## View Results

```bash
# View summary
cat test-results/reports/LATEST-SUMMARY.md

# View all action items (todos)
cat test-results/reports/test-report-*-todos.md

# View full report
cat test-results/reports/test-report-*.md

# View logs
cat test-results/logs/backend-*.log
cat test-results/logs/e2e-*.log
cat test-results/logs/roadmap-*.log
```

## Common Workflows

### Daily Quality Check

```bash
bash scripts/comprehensive-test-reporter.sh --all && \
echo "✅ All tests passed!" || \
(echo "❌ Some tests failed. Review:" && \
cat test-results/reports/test-report-*-todos.md)
```

### Fix Tests in Specific Module

```bash
# Run only backend tests
bash scripts/comprehensive-test-reporter.sh --backend

# See what failed in graphql-service
grep -A 5 "graphql-service" test-results/reports/test-report-*-todos.md
```

### Pre-Release Testing

```bash
bash scripts/comprehensive-test-reporter.sh --all --verbose

# Check pass rate
if grep -q "100%" test-results/reports/LATEST-SUMMARY.md; then
  echo "Ready for release!"
else
  echo "Fix failures before release"
  cat test-results/reports/test-report-*-todos.md
fi
```

## Todo Priority & Deadlines

| Level | Deadline | What to Do |
|-------|----------|-----------|
| 🔴 **Critical** | **Same Day** | **Drop everything, fix now** |
| 🟠 **High** | **1-2 Days** | Fix this sprint |
| 🟡 **Medium** | **3 Days** | Plan for this week |
| 🟢 **Low** | **1 Week** | Next sprint |

## Report Contents

### Summary Report (60 seconds to read)
```
✅ Quick stats
✅ Pass rate
✅ Action item count
✅ Links to detailed reports
```

### Full Report (5-10 minutes to read)
```
✅ Executive summary
✅ Results by test type
✅ Statistics
✅ Links to logs
```

### Todo Report (decision making)
```
✅ All failures as actionable todos
✅ Organized by severity
✅ Includes deadlines
✅ Ready to add to task tracker
```

## File Locations

```
/workspaces/Graphite-Forge/
├── scripts/
│   └── comprehensive-test-reporter.sh    ← Run this
├── test-results/
│   ├── reports/                          ← View these
│   │   ├── LATEST-SUMMARY.md
│   │   ├── test-report-*.md
│   │   ├── test-report-*-todos.md
│   │   └── test-report-*.json
│   └── logs/                             ← Debug these
│       ├── backend-*.log
│       ├── e2e-*.log
│       └── roadmap-*.log
└── docs/
    └── TEST-REPORTING-SYSTEM.md          ← Read this for full docs
```

## Troubleshooting

**Issue**: No reports generated
```bash
# Check directory exists
ls -la test-results/reports/

# Check permissions
chmod 755 test-results/reports/
```

**Issue**: Tests won't run
```bash
# Check Maven installed
mvn --version

# Check Node installed  
npm --version

# Run verbose to see errors
bash scripts/comprehensive-test-reporter.sh --all --verbose
```

**Issue**: Want to see what failed
```bash
# View todo items
cat test-results/reports/test-report-*-todos.md

# View error details
grep -i "error\|failed" test-results/logs/backend-*.log
```

## Integration Examples

### Add to Git Pre-Commit Hook

```bash
#!/bin/bash
# Save as .git/hooks/pre-commit
bash scripts/comprehensive-test-reporter.sh --backend
exit $?
```

### Add to GitHub Actions

```yaml
- name: Run Tests
  run: bash scripts/comprehensive-test-reporter.sh --all
  
- name: Upload Reports
  uses: actions/upload-artifact@v2
  with:
    name: test-reports
    path: test-results/
```

### Slack Notification

```bash
bash scripts/comprehensive-test-reporter.sh --all

# Send to Slack
SUMMARY=$(cat test-results/reports/LATEST-SUMMARY.md)
curl -X POST $SLACK_WEBHOOK -d "{\"text\": \"$SUMMARY\"}"
```

## Performance Notes

Typical execution times:
- Backend tests: 2-5 minutes (Maven)
- E2E tests: 5-10 minutes (depends on system)
- Roadmap tests: 30 seconds (Jest)
- Report generation: <5 seconds
- **Total**: Usually 10-20 minutes

To speed up:
```bash
# Run only what you need
bash scripts/comprehensive-test-reporter.sh --backend  # Fast!
```

## Pro Tips

1. **Bookmark the summary**: Open latest-summary.md every morning
2. **Copy todos to tracker**: Paste todo report into Jira/GitHub Issues
3. **Trend analysis**: Compare old vs new reports to see progress
4. **Team dashboard**: Share JSON report with team dashboard
5. **Automate it**: Add to CI/CD pipeline for automatic reporting

## Full Documentation

For complete documentation, see:
```
docs/TEST-REPORTING-SYSTEM.md
```

## Support

Questions? Check these locations:
1. `docs/TEST-REPORTING-SYSTEM.md` - Full documentation
2. `test-results/logs/` - Error logs
3. Run with `--verbose` flag for detailed output

---

**Remember**: The faster you fix critical failures, the better your code quality! 🚀
