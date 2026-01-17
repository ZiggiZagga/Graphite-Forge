# Test Reporting & Todo System Documentation

## Overview

This comprehensive test reporting system automatically captures all failing tests and converts them into well-structured, actionable todo items. It provides multiple report formats for different audiences and integrates with all test types in the Graphite-Forge project.

## Key Features

✅ **Unified Test Execution**
- Runs backend tests (Maven)
- Runs E2E tests (shell scripts)
- Runs roadmap tests (Jest)
- Flexible selection of which tests to run

✅ **Automatic Todo Generation**
- Converts each failing test into an actionable todo
- Organizes by severity (Critical, High, Medium, Low)
- Includes deadlines based on severity
- Tracks blocking status

✅ **Multiple Report Formats**
- **Markdown Report** - Human-readable comprehensive summary
- **Todo Report** - Failures as structured action items
- **JSON Report** - Machine-readable format for tooling
- **Summary Report** - Quick reference to latest reports

✅ **Comprehensive Logging**
- Color-coded console output
- Detailed log files for each test type
- Verbose mode for debugging
- Structured failure tracking

## Quick Start

### Run All Tests with Reporting

```bash
cd /workspaces/Graphite-Forge
bash scripts/comprehensive-test-reporter.sh --all
```

### Run Specific Test Types

```bash
# Backend tests only
bash scripts/comprehensive-test-reporter.sh --backend

# E2E tests only
bash scripts/comprehensive-test-reporter.sh --e2e

# Roadmap tests only
bash scripts/comprehensive-test-reporter.sh --roadmap

# Combination
bash scripts/comprehensive-test-reporter.sh --backend --roadmap --verbose
```

### View Generated Reports

After execution, reports are available in `test-results/reports/`:

```bash
# View latest summary
cat test-results/reports/LATEST-SUMMARY.md

# View detailed todo list
cat test-results/reports/test-report-TIMESTAMP-todos.md

# View full markdown report
cat test-results/reports/test-report-TIMESTAMP.md

# View JSON for tooling
cat test-results/reports/test-report-TIMESTAMP.json
```

## System Components

### 1. Main Reporter Script

**File**: `scripts/comprehensive-test-reporter.sh`

The orchestrating bash script that:
- Executes all test suites
- Captures failures
- Generates multiple report formats
- Manages directories and logging

**Key Functions**:
```bash
run_backend_tests()      # Maven test execution
run_e2e_tests()          # E2E test execution
run_roadmap_tests()      # Jest test execution
generate_json_report()   # JSON format
generate_markdown_report() # Markdown format
generate_todo_report()   # Todo format with actionable items
generate_summary_report() # Quick reference
```

### 2. TypeScript Reporter Class

**File**: `scripts/test-reporter.ts`

Advanced reporting capabilities for Node.js/TypeScript projects:
- Structured test failure objects
- HTML report generation
- Severity determination algorithms
- Deadline calculation
- Module extraction from file paths

**Key Classes**:
```typescript
class TestReporter {
  addTestSuite(suiteResult): void
  generateJsonReport(): string
  generateMarkdownReport(): string
  generateHtmlReport(): string
  generateTodoReport(): string
  generateAllReports(): object
}
```

### 3. Jest Reporter Integration

**File**: `scripts/test-reporter-jest.ts`

Jest custom reporter that:
- Captures test results in real-time
- Integrates with Jest lifecycle
- Feeds failures to reporting system
- Determines severity levels

**Usage in Jest Config**:
```javascript
reporters: [
  'default',
  './scripts/test-reporter-jest.ts'
]
```

## Report Formats

### 1. Markdown Report (test-report-TIMESTAMP.md)

Comprehensive summary with:
- Executive summary with statistics
- Results by test type
- Pass rates and failure counts
- Links to log files
- Human-readable formatting

**Example**:
```markdown
# Test Execution Report

**Generated**: Mon Dec 19 2024 10:30:45 GMT

## 📊 Executive Summary

| Metric | Value |
|--------|-------|
| Total Tests | 250 |
| Passed | 245 |
| Failed | 5 |
| Pass Rate | 98% |

## 🧪 Test Results by Type

### Backend Tests (Maven)
- **Status**: ✅ Passed
- **Failures**: 0

...
```

### 2. Todo Report (test-report-TIMESTAMP-todos.md)

Structured action items for developers:
- Organized by severity level
- Includes todo ID, title, deadline
- Tracks blocking status
- Module assignment
- Error messages

**Example**:
```markdown
# Test Failure Todo List

## 🔴 Critical Priority Todos

### [1 Critical] Maven tests failed in graphql-service

[ ] Todo ID: CRITICAL-1
[ ] Title: Fix - Maven tests failed in graphql-service
[ ] Severity: CRITICAL 🔴
[ ] Module: graphql-service
[ ] Priority: ASAP
[ ] Deadline: Same Day
[ ] Assignee: [Unassigned]

**Action**: Investigate failure immediately...

---

## 🟠 High Priority Todos
...
```

### 3. JSON Report (test-report-TIMESTAMP.json)

Machine-readable format for integration:
- Test statistics
- Failure counts by type
- Log file paths
- Metadata
- Timestamps

**Example**:
```json
{
  "metadata": {
    "timestamp": "2024-12-19T10:30:45Z",
    "reportId": "test-report-20241219_103045",
    "generator": "Comprehensive Test Reporting System"
  },
  "summary": {
    "totalTests": 250,
    "passed": 245,
    "failed": 5,
    "passRate": 98
  },
  "failures": {
    "backend": 2,
    "e2e": 2,
    "roadmap": 1
  }
}
```

### 4. Summary Report (LATEST-SUMMARY.md)

Quick reference with links to all reports:
- Current pass rate
- Test type status
- Links to detailed reports
- Action item count

## Severity Levels & Deadlines

Failures are automatically categorized:

| Severity | Keywords | Deadline | Priority |
|----------|----------|----------|----------|
| **Critical** | auth, security, payment, database, api | Same Day | Blocking |
| **High** | integration, timeout | 1-2 Days | Current Sprint |
| **Medium** | unit tests, assertion errors | 3 Days | This Week |
| **Low** | other failures | 1 Week | Next Sprint |

## Todo Structure

Each todo item includes:

```
[ ] Todo ID: [SEVERITY-NUMBER]
[ ] Title: Fix - [Test Name]
[ ] Severity: [CRITICAL/HIGH/MEDIUM/LOW]
[ ] Module: [Service Name]
[ ] Priority: [ASAP/Current Sprint/This Week/Next Sprint]
[ ] Deadline: [Date or Duration]
[ ] Status: [Open/In Progress/Resolved]
[ ] Assignee: [Optional]
```

## Directory Structure

```
test-results/
├── reports/
│   ├── test-report-20241219_103045.md
│   ├── test-report-20241219_103045-todos.md
│   ├── test-report-20241219_103045.json
│   └── LATEST-SUMMARY.md
└── logs/
    ├── backend-20241219_103045.log
    ├── e2e-20241219_103045.log
    └── roadmap-20241219_103045.log
```

## Configuration Options

### Command Line Flags

```bash
# Test selection
--backend       Run only backend Maven tests
--e2e          Run only E2E tests  
--roadmap      Run only Jest roadmap tests
--all          Run all tests (default)

# Output control
--verbose      Show detailed output during execution
--help         Display usage information
```

### Environment Variables

```bash
# Override report directory
TEST_RESULTS_DIR=/custom/path

# Control logging
VERBOSE=true

# Debug mode
DEBUG=true
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Test & Report

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run Tests & Generate Reports
        run: bash scripts/comprehensive-test-reporter.sh --all
      - name: Upload Reports
        uses: actions/upload-artifact@v2
        with:
          name: test-reports
          path: test-results/reports/
      - name: Comment with Todo Summary
        if: always()
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const todoReport = fs.readFileSync('test-results/reports/LATEST-SUMMARY.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: todoReport
            });
```

## Usage Examples

### Example 1: Daily Test Run

```bash
#!/bin/bash
cd /workspaces/Graphite-Forge
bash scripts/comprehensive-test-reporter.sh --all

# View results
echo "=== Test Summary ==="
cat test-results/reports/LATEST-SUMMARY.md

# If failures, show todos
if grep -q "Action Items" test-results/reports/LATEST-SUMMARY.md; then
  echo ""
  echo "=== Action Items ==="
  cat test-results/reports/test-report-*-todos.md
fi
```

### Example 2: Pre-Commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

bash scripts/comprehensive-test-reporter.sh --roadmap --backend

if [ $? -ne 0 ]; then
  echo "Tests failed! Review todos:"
  cat test-results/reports/test-report-*-todos.md
  exit 1
fi
```

### Example 3: Selective Testing During Development

```bash
# Only test the module you're working on
bash scripts/comprehensive-test-reporter.sh --backend --verbose

# Review todos for graphql-service
grep -A 10 "graphql-service" test-results/reports/test-report-*-todos.md
```

## Troubleshooting

### No reports generated
- Check that `test-results/reports/` directory is writable
- Verify disk space availability
- Check for permission errors in logs

### Tests not running
- Verify Maven is installed (`mvn --version`)
- Check Node.js/npm installed for roadmap tests (`npm --version`)
- Ensure test scripts exist at expected locations

### Todos not being generated
- Check that failures are being captured (view logs)
- Verify failure detection regex patterns
- Run with `--verbose` flag for detailed output

## Advanced Usage

### Custom Report Generation

```bash
# Generate only specific reports
cd scripts

# Use TypeScript reporter directly (after compilation)
npx ts-node test-reporter.ts

# Parse specific test output
grep -E "FAILED|ERROR" ../test-results/logs/backend-*.log
```

### Metrics & Trending

```bash
# Track test results over time
for report in test-results/reports/test-report-*.json; do
  jq '.summary | {timestamp: .timestamp, passRate: .passRate}' "$report"
done

# Generate trend chart
# (Use external tools like gnuplot or upload to dashboard)
```

## Best Practices

1. **Run Tests Regularly**
   - Daily in CI/CD pipeline
   - Before commits on feature branches
   - Before releases

2. **Address Todos Promptly**
   - Critical: Same day
   - High: Next 1-2 days
   - Medium: This week
   - Low: Next sprint

3. **Keep Reports Organized**
   - Archive old reports periodically
   - Store in version control
   - Link from project wiki/docs

4. **Monitor Trends**
   - Track pass rate over time
   - Identify patterns in failures
   - Allocate resources to high-failure areas

5. **Integrate with Workflow**
   - Add to CI/CD pipelines
   - Link with issue tracking (GitHub/Jira)
   - Notify team via Slack/email
   - Add to sprint planning

## Support & Feedback

For issues or improvements:
1. Check the troubleshooting section
2. Review log files in `test-results/logs/`
3. Run with `--verbose` flag
4. Submit feedback to the development team

## License

Part of the Graphite-Forge project testing infrastructure.
