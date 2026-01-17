# Test Reporting System - CI/CD Integration Guide

## Overview

This guide shows how to integrate the comprehensive test reporting system with CI/CD pipelines to automatically generate todo reports from failing tests.

## Quick Integration (5 minutes)

### GitHub Actions

Add to `.github/workflows/test.yml`:

```yaml
name: Test & Report

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Run All Tests & Generate Reports
        run: |
          cd /workspaces/Graphite-Forge
          bash scripts/comprehensive-test-reporter.sh --all
        continue-on-error: true
      
      - name: Upload Test Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-reports
          path: test-results/reports/
      
      - name: Upload Test Logs
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-logs
          path: test-results/logs/
      
      - name: Comment with Todo Report (PR)
        if: github.event_name == 'pull_request' && always()
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const path = require('path');
            
            // Find the latest todo report
            const reportsDir = 'test-results/reports';
            const files = fs.readdirSync(reportsDir);
            const todoFile = files.find(f => f.includes('-todos.md'));
            
            if (todoFile) {
              const todoReport = fs.readFileSync(
                path.join(reportsDir, todoFile), 
                'utf8'
              );
              
              github.rest.issues.createComment({
                issue_number: context.issue.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
                body: '## 📋 Test Results & Todos\n\n' + todoReport
              });
            }
      
      - name: Check Test Status
        run: |
          if [ -f "test-results/reports/LATEST-SUMMARY.md" ]; then
            cat test-results/reports/LATEST-SUMMARY.md
            if grep -q "❌" test-results/reports/LATEST-SUMMARY.md; then
              exit 1
            fi
          fi
```

### GitLab CI

Add to `.gitlab-ci.yml`:

```yaml
test-and-report:
  stage: test
  image: ubuntu:24.04
  
  before_script:
    - apt-get update
    - apt-get install -y maven openjdk-17-jdk nodejs npm git
  
  script:
    - cd /workspaces/Graphite-Forge
    - bash scripts/comprehensive-test-reporter.sh --all
  
  artifacts:
    paths:
      - test-results/reports/
      - test-results/logs/
    reports:
      junit: test-results/reports/*.json
    expire_in: 30 days
  
  allow_failure: true
```

### Jenkins

Add to `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Test & Report') {
            steps {
                dir('/workspaces/Graphite-Forge') {
                    sh 'bash scripts/comprehensive-test-reporter.sh --all'
                }
            }
            post {
                always {
                    // Archive reports
                    archiveArtifacts artifacts: 'test-results/**/*', 
                                     allowEmptyArchive: true
                    
                    // Publish JUnit results
                    junit 'test-results/reports/*.json'
                    
                    // Show summary
                    script {
                        def summary = readFile 'test-results/reports/LATEST-SUMMARY.md'
                        echo summary
                    }
                }
                failure {
                    script {
                        def todos = readFile 'test-results/reports/test-report-*-todos.md'
                        echo "Failed tests converted to todos:"
                        echo todos
                    }
                }
            }
        }
    }
}
```

## Advanced Integrations

### Slack Notifications

```bash
#!/bin/bash
# scripts/notify-slack.sh

SLACK_WEBHOOK=$1

if [ -z "$SLACK_WEBHOOK" ]; then
  echo "Usage: notify-slack.sh <webhook-url>"
  exit 1
fi

# Run tests
bash scripts/comprehensive-test-reporter.sh --all

# Read summary
SUMMARY=$(cat test-results/reports/LATEST-SUMMARY.md)

# Count todos
TODO_COUNT=$(grep -c "^\[ \]" test-results/reports/test-report-*-todos.md || echo "0")

# Create Slack message
SLACK_MESSAGE=$(cat <<EOF
{
  "text": "Test Report: $(date)",
  "blocks": [
    {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": "📊 Test Execution Report"
      }
    },
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "$SUMMARY"
      }
    },
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*Action Items*: $TODO_COUNT"
      }
    }
  ]
}
EOF
)

# Send to Slack
curl -X POST "$SLACK_WEBHOOK" \
  -H 'Content-type: application/json' \
  --data "$SLACK_MESSAGE"
```

### Email Reporting

```bash
#!/bin/bash
# scripts/send-test-report-email.sh

RECIPIENT=$1

if [ -z "$RECIPIENT" ]; then
  echo "Usage: send-test-report-email.sh <email@example.com>"
  exit 1
fi

# Run tests
bash scripts/comprehensive-test-reporter.sh --all

# Create email body
{
  echo "Test Execution Report"
  echo "$(date)"
  echo ""
  cat test-results/reports/LATEST-SUMMARY.md
  echo ""
  echo "---"
  echo "Action Items:"
  echo ""
  cat test-results/reports/test-report-*-todos.md
} | mail -s "Test Report - $(date +%Y-%m-%d)" "$RECIPIENT"
```

### Dashboard Integration

```javascript
// Create dashboard from JSON reports
const fs = require('fs');
const path = require('path');

function loadReports(reportsDir) {
  const files = fs.readdirSync(reportsDir)
    .filter(f => f.endsWith('.json'))
    .sort()
    .reverse()
    .slice(0, 30); // Last 30 reports
  
  return files.map(file => {
    const data = JSON.parse(
      fs.readFileSync(path.join(reportsDir, file), 'utf8')
    );
    return {
      date: data.metadata.timestamp,
      passRate: data.summary.passRate,
      failed: data.summary.failed,
      total: data.summary.totalTests
    };
  });
}

function generateDashboard(reportsDir) {
  const reports = loadReports(reportsDir);
  
  return {
    current: reports[0],
    trend: {
      passRateChange: reports[0].passRate - (reports[1]?.passRate || 0),
      failureCount: reports[0].failed
    },
    history: reports
  };
}

module.exports = { loadReports, generateDashboard };
```

## Automation Scripts

### Daily Test Report Cron Job

```bash
#!/bin/bash
# scripts/daily-test-report.sh

PROJECT_DIR="/workspaces/Graphite-Forge"
REPORT_DIR="$PROJECT_DIR/test-results/reports"

cd "$PROJECT_DIR"

# Run tests
bash scripts/comprehensive-test-reporter.sh --all

# Archive report
TIMESTAMP=$(date +%Y-%m-%d)
mkdir -p "reports-archive/$TIMESTAMP"
cp "$REPORT_DIR"/*.md "reports-archive/$TIMESTAMP/"
cp "$REPORT_DIR"/*.json "reports-archive/$TIMESTAMP/"

# Send to team
bash scripts/notify-slack.sh "$SLACK_WEBHOOK"

echo "Daily test report generated and archived"
```

Add to crontab:
```
0 6 * * * /workspaces/Graphite-Forge/scripts/daily-test-report.sh >> /var/log/test-reports.log 2>&1
```

### Pre-Commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

PROJECT_DIR="$(git rev-parse --show-toplevel)"

echo "🧪 Running pre-commit tests..."

cd "$PROJECT_DIR"

# Run quick tests only (backend and roadmap, skip E2E)
bash scripts/comprehensive-test-reporter.sh --backend --roadmap

if [ $? -ne 0 ]; then
  echo ""
  echo "❌ Tests failed! Review action items:"
  echo ""
  cat test-results/reports/test-report-*-todos.md
  echo ""
  echo "Fix the issues above before committing."
  exit 1
fi

echo "✅ All tests passed!"
exit 0
```

Install:
```bash
chmod +x scripts/comprehensive-test-reporter.sh
cp scripts/comprehensive-test-reporter.sh .git/hooks/pre-commit
```

### Test Report Dashboard (Express.js)

```javascript
const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const REPORTS_DIR = '../test-results/reports';

app.get('/api/test-reports', (req, res) => {
  const files = fs.readdirSync(REPORTS_DIR)
    .filter(f => f.endsWith('.json'))
    .sort()
    .reverse();
  
  const reports = files.slice(0, 10).map(file => {
    const data = JSON.parse(
      fs.readFileSync(path.join(REPORTS_DIR, file), 'utf8')
    );
    return data;
  });
  
  res.json(reports);
});

app.get('/api/latest-todos', (req, res) => {
  const files = fs.readdirSync(REPORTS_DIR)
    .filter(f => f.includes('-todos.md'))
    .sort()
    .reverse();
  
  if (files.length > 0) {
    const todos = fs.readFileSync(
      path.join(REPORTS_DIR, files[0]), 
      'utf8'
    );
    res.header('Content-Type', 'text/markdown');
    res.send(todos);
  } else {
    res.status(404).send('No todo reports found');
  }
});

app.listen(3000, () => {
  console.log('Test Report Dashboard running on port 3000');
});
```

## Integration Checklist

- [ ] Add test reporting to CI/CD pipeline
- [ ] Configure artifact upload
- [ ] Set up PR comments with todo reports
- [ ] Enable email notifications
- [ ] Create Slack channel for reports
- [ ] Set up daily summary emails
- [ ] Configure pre-commit hooks locally
- [ ] Archive reports for trending
- [ ] Create team dashboard
- [ ] Document for team

## Troubleshooting

**Reports not uploading to CI/CD**
```bash
# Check artifact path
ls -la test-results/reports/

# Verify path in CI/CD config
# Should match: test-results/reports/**/*
```

**Slack messages not sending**
```bash
# Test webhook
curl -X POST "$SLACK_WEBHOOK" \
  -d '{"text": "Test message"}'
```

**Missing dependencies in CI**
```yaml
# Add to CI setup
install:
  - apt-get update
  - apt-get install -y maven openjdk-17-jdk nodejs npm
```

## Best Practices

1. **Run tests on every push** - Early detection of issues
2. **Archive reports** - Track trends over time
3. **Notify team** - Quick response to failures
4. **Pre-commit validation** - Prevent breaking changes
5. **Daily summaries** - Keep team aware
6. **Dashboard monitoring** - Visual trend analysis
7. **Automated todos** - Structured task management

## Next Steps

1. Choose your CI/CD platform
2. Copy the relevant configuration
3. Update paths for your environment
4. Test with a sample run
5. Integrate with team communication
6. Monitor and adjust as needed

---

For more information, see:
- [Test Reporting System Documentation](TEST-REPORTING-SYSTEM.md)
- [Quick Reference Guide](TEST-REPORTING-QUICK-REFERENCE.md)
- [Sample Reports](SAMPLE-TEST-REPORT.md)
