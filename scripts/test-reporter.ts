/**
 * Test Reporting System - TypeScript/JavaScript Implementation
 * Generates well-structured test failure reports with todo lists
 * 
 * Features:
 * - Captures test failures as actionable todos
 * - Generates JSON, Markdown, and HTML reports
 * - Organizes by severity and priority
 * - Integration with Jest reporters
 */

import * as fs from 'fs';
import * as path from 'path';

interface TestFailure {
  name: string;
  suite: string;
  error: string;
  stack?: string;
  severity: 'critical' | 'high' | 'medium' | 'low';
  module: string;
  isBlocking: boolean;
}

interface TestResult {
  name: string;
  suite: string;
  status: 'passed' | 'failed' | 'skipped';
  duration: number;
  error?: string;
}

interface TestSuiteResult {
  name: string;
  path: string;
  total: number;
  passed: number;
  failed: number;
  skipped: number;
  duration: number;
  tests: TestResult[];
  failedTests: TestFailure[];
}

interface ReportData {
  timestamp: string;
  reportId: string;
  summary: {
    totalTests: number;
    passed: number;
    failed: number;
    skipped: number;
    passRate: number;
    duration: number;
  };
  testSuites: TestSuiteResult[];
  failingTests: TestFailure[];
  criticalTodos: Todo[];
  highPriorityTodos: Todo[];
  mediumPriorityTodos: Todo[];
}

interface Todo {
  id: string;
  title: string;
  testName: string;
  severity: string;
  module: string;
  error: string;
  assignedTo?: string;
  deadline?: string;
  isBlocking: boolean;
  relatedIssues?: string[];
}

/**
 * Main Test Reporter Class
 */
export class TestReporter {
  private reportDir: string;
  private timestamp: string;
  private reportId: string;
  private testSuites: Map<string, TestSuiteResult> = new Map();

  constructor(reportDir: string = './test-results/reports') {
    this.reportDir = reportDir;
    this.timestamp = new Date().toISOString();
    this.reportId = `test-report-${new Date().toISOString().split('T')[0]}_${Date.now()}`;
    this.ensureDirectories();
  }

  /**
   * Ensure report directories exist
   */
  private ensureDirectories(): void {
    if (!fs.existsSync(this.reportDir)) {
      fs.mkdirSync(this.reportDir, { recursive: true });
    }
  }

  /**
   * Add a test suite result
   */
  addTestSuite(suiteResult: TestSuiteResult): void {
    this.testSuites.set(suiteResult.name, suiteResult);
  }

  /**
   * Generate comprehensive report data
   */
  private generateReportData(): ReportData {
    let totalTests = 0;
    let totalPassed = 0;
    let totalFailed = 0;
    let totalSkipped = 0;
    let totalDuration = 0;
    const allFailingTests: TestFailure[] = [];
    const allTodos: Todo[] = [];

    for (const suite of this.testSuites.values()) {
      totalTests += suite.total;
      totalPassed += suite.passed;
      totalFailed += suite.failed;
      totalSkipped += suite.skipped;
      totalDuration += suite.duration;
      allFailingTests.push(...suite.failedTests);
    }

    // Generate todos from failing tests
    allFailingTests.forEach((failure, index) => {
      const todo = this.createTodoFromFailure(failure, index);
      allTodos.push(todo);
    });

    // Sort todos by severity
    const criticalTodos = allTodos.filter((t) => t.severity === 'critical');
    const highTodos = allTodos.filter((t) => t.severity === 'high');
    const mediumTodos = allTodos.filter((t) => t.severity === 'medium');

    return {
      timestamp: this.timestamp,
      reportId: this.reportId,
      summary: {
        totalTests,
        passed: totalPassed,
        failed: totalFailed,
        skipped: totalSkipped,
        passRate: totalTests > 0 ? (totalPassed / totalTests) * 100 : 0,
        duration: totalDuration,
      },
      testSuites: Array.from(this.testSuites.values()),
      failingTests: allFailingTests,
      criticalTodos,
      highPriorityTodos: highTodos,
      mediumPriorityTodos: mediumTodos,
    };
  }

  /**
   * Create a todo item from a test failure
   */
  private createTodoFromFailure(failure: TestFailure, index: number): Todo {
    return {
      id: `todo-${index + 1}`,
      title: `Fix failing test: ${failure.name}`,
      testName: failure.name,
      severity: failure.severity.toUpperCase(),
      module: failure.module,
      error: failure.error,
      isBlocking: failure.isBlocking,
      assignedTo: undefined, // Can be set by user
      deadline: this.getDeadlineForSeverity(failure.severity),
    };
  }

  /**
   * Get deadline based on severity
   */
  private getDeadlineForSeverity(severity: string): string {
    const today = new Date();
    switch (severity) {
      case 'critical':
        return 'ASAP'; // Same day
      case 'high':
        today.setDate(today.getDate() + 1);
        return today.toISOString().split('T')[0]; // Next day
      case 'medium':
        today.setDate(today.getDate() + 3);
        return today.toISOString().split('T')[0]; // 3 days
      default:
        today.setDate(today.getDate() + 7);
        return today.toISOString().split('T')[0]; // 1 week
    }
  }

  /**
   * Generate JSON report
   */
  generateJsonReport(): string {
    const data = this.generateReportData();
    const filePath = path.join(this.reportDir, `${this.reportId}.json`);

    fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
    console.log(`✓ JSON report created: ${filePath}`);

    return filePath;
  }

  /**
   * Generate Markdown report
   */
  generateMarkdownReport(): string {
    const data = this.generateReportData();
    const filePath = path.join(this.reportDir, `${this.reportId}.md`);

    let markdown = `# Test Execution Report\n\n`;
    markdown += `**Generated**: ${new Date(this.timestamp).toLocaleString()}\n`;
    markdown += `**Report ID**: ${this.reportId}\n\n`;

    // Summary section
    markdown += `## 📊 Executive Summary\n\n`;
    markdown += `| Metric | Value |\n`;
    markdown += `|--------|-------|\n`;
    markdown += `| Total Tests | ${data.summary.totalTests} |\n`;
    markdown += `| Passed | ${data.summary.passed} |\n`;
    markdown += `| Failed | ${data.summary.failed} |\n`;
    markdown += `| Skipped | ${data.summary.skipped} |\n`;
    markdown += `| Pass Rate | ${data.summary.passRate.toFixed(2)}% |\n`;
    markdown += `| Duration | ${(data.summary.duration / 1000).toFixed(2)}s |\n\n`;

    // Results by suite
    markdown += `## 🧪 Test Results by Suite\n\n`;
    for (const suite of data.testSuites) {
      markdown += `### ${suite.name}\n\n`;
      markdown += `- **Path**: ${suite.path}\n`;
      markdown += `- **Total**: ${suite.total} | **Passed**: ${suite.passed} | **Failed**: ${suite.failed}\n`;
      markdown += `- **Duration**: ${(suite.duration / 1000).toFixed(2)}s\n\n`;
    }

    // Failed tests section
    if (data.failingTests.length > 0) {
      markdown += `## ❌ Failed Tests\n\n`;
      for (const failure of data.failingTests) {
        markdown += `### ${failure.name}\n\n`;
        markdown += `- **Suite**: ${failure.suite}\n`;
        markdown += `- **Module**: ${failure.module}\n`;
        markdown += `- **Severity**: ${failure.severity.toUpperCase()}\n`;
        markdown += `- **Error**: \`${failure.error}\`\n`;
        if (failure.stack) {
          markdown += `- **Stack**:\n\`\`\`\n${failure.stack}\n\`\`\`\n`;
        }
        markdown += `\n`;
      }
    }

    fs.writeFileSync(filePath, markdown);
    console.log(`✓ Markdown report created: ${filePath}`);

    return filePath;
  }

  /**
   * Generate TODO report with structured failure items
   */
  generateTodoReport(): string {
    const data = this.generateReportData();
    const filePath = path.join(this.reportDir, `${this.reportId}-todos.md`);

    let markdown = `# Test Failure Todo List\n\n`;
    markdown += `**Generated**: ${new Date(this.timestamp).toLocaleString()}\n`;
    markdown += `**Report ID**: ${this.reportId}\n`;
    markdown += `**Priority**: Action Required\n\n`;

    // Critical todos
    if (data.criticalTodos.length > 0) {
      markdown += `## 🔴 Critical Failures (Blocking)\n\n`;
      markdown += `These failures block other work and must be fixed immediately.\n\n`;

      for (const todo of data.criticalTodos) {
        markdown += this.formatTodoItem(todo);
      }
    }

    // High priority todos
    if (data.highPriorityTodos.length > 0) {
      markdown += `## 🟠 High Priority Failures\n\n`;
      markdown += `These should be fixed within the current sprint.\n\n`;

      for (const todo of data.highPriorityTodos) {
        markdown += this.formatTodoItem(todo);
      }
    }

    // Medium priority todos
    if (data.mediumPriorityTodos.length > 0) {
      markdown += `## 🟡 Medium Priority Failures\n\n`;
      markdown += `These should be fixed when resources are available.\n\n`;

      for (const todo of data.mediumPriorityTodos) {
        markdown += this.formatTodoItem(todo);
      }
    }

    // Statistics
    markdown += `## 📈 Statistics\n\n`;
    markdown += `- **Total Todos**: ${data.criticalTodos.length + data.highPriorityTodos.length + data.mediumPriorityTodos.length}\n`;
    markdown += `- **Critical**: ${data.criticalTodos.length}\n`;
    markdown += `- **High**: ${data.highPriorityTodos.length}\n`;
    markdown += `- **Medium**: ${data.mediumPriorityTodos.length}\n\n`;

    // By module
    markdown += `## 📁 By Module\n\n`;
    const moduleMap = new Map<string, Todo[]>();
    for (const todo of [...data.criticalTodos, ...data.highPriorityTodos, ...data.mediumPriorityTodos]) {
      if (!moduleMap.has(todo.module)) {
        moduleMap.set(todo.module, []);
      }
      moduleMap.get(todo.module)!.push(todo);
    }

    for (const [module, todos] of moduleMap) {
      markdown += `### ${module}\n\n`;
      for (const todo of todos) {
        markdown += `- [ ] **[${todo.severity}]** ${todo.title}\n`;
      }
      markdown += `\n`;
    }

    fs.writeFileSync(filePath, markdown);
    console.log(`✓ TODO report created: ${filePath}`);

    return filePath;
  }

  /**
   * Format a todo item as markdown
   */
  private formatTodoItem(todo: Todo): string {
    let markdown = `### ${todo.testName}\n\n`;
    markdown += `- [ ] **${todo.id}**: ${todo.title}\n`;
    markdown += `  - **Severity**: ${todo.severity}\n`;
    markdown += `  - **Module**: ${todo.module}\n`;
    markdown += `  - **Error**: \`${todo.error}\`\n`;
    markdown += `  - **Blocking**: ${todo.isBlocking ? 'Yes ⚠️' : 'No'}\n`;
    if (todo.deadline) {
      markdown += `  - **Deadline**: ${todo.deadline}\n`;
    }
    if (todo.assignedTo) {
      markdown += `  - **Assigned to**: @${todo.assignedTo}\n`;
    }
    if (todo.relatedIssues && todo.relatedIssues.length > 0) {
      markdown += `  - **Related Issues**: ${todo.relatedIssues.join(', ')}\n`;
    }
    markdown += `\n`;
    return markdown;
  }

  /**
   * Generate HTML report
   */
  generateHtmlReport(): string {
    const data = this.generateReportData();
    const filePath = path.join(this.reportDir, `${this.reportId}.html`);

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Execution Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; background: #f5f5f5; color: #333; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px; margin-bottom: 30px; }
        h1 { font-size: 2em; margin-bottom: 10px; }
        .metadata { font-size: 0.9em; opacity: 0.9; }
        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .summary-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .summary-card h3 { color: #667eea; font-size: 0.9em; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }
        .summary-card .value { font-size: 2em; font-weight: bold; }
        .summary-card .unit { font-size: 0.8em; color: #999; }
        .test-suites { margin-bottom: 30px; }
        .test-suite { background: white; padding: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .test-suite h2 { color: #667eea; margin-bottom: 15px; }
        .test-suite-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
        .stat { text-align: center; }
        .stat .label { font-size: 0.85em; color: #999; text-transform: uppercase; }
        .stat .value { font-size: 1.5em; font-weight: bold; }
        .failed-tests { margin-bottom: 30px; }
        .failed-test { background: #fff5f5; padding: 15px; margin-bottom: 15px; border-left: 4px solid #ff6b6b; border-radius: 4px; }
        .failed-test h3 { color: #c92a2a; margin-bottom: 10px; }
        .failed-test .error { background: white; padding: 10px; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 0.85em; overflow-x: auto; }
        .todos { margin-bottom: 30px; }
        .todo-section { margin-bottom: 30px; }
        .todo-section h2 { margin-bottom: 15px; }
        .todo-critical { border-left: 4px solid #c92a2a; background: #fff5f5; }
        .todo-high { border-left: 4px solid #ff922b; background: #fff8f0; }
        .todo-medium { border-left: 4px solid #fcc419; background: #fffbf0; }
        .todo-item { background: white; padding: 15px; margin-bottom: 10px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .todo-item.todo-critical { border-left: 4px solid #c92a2a; }
        .todo-item.todo-high { border-left: 4px solid #ff922b; }
        .todo-item.todo-medium { border-left: 4px solid #fcc419; }
        .todo-item input[type="checkbox"] { margin-right: 10px; }
        .todo-item .todo-title { font-weight: bold; margin-bottom: 8px; }
        .todo-item .todo-meta { font-size: 0.85em; color: #666; }
        .todo-item .todo-meta span { margin-right: 15px; }
        footer { text-align: center; color: #999; margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>Test Execution Report</h1>
            <div class="metadata">
                <p>Generated: ${new Date(data.timestamp).toLocaleString()}</p>
                <p>Report ID: ${data.reportId}</p>
            </div>
        </header>

        <div class="summary-grid">
            <div class="summary-card">
                <h3>Total Tests</h3>
                <div class="value">${data.summary.totalTests}</div>
            </div>
            <div class="summary-card">
                <h3>Passed</h3>
                <div class="value" style="color: #51cf66;">${data.summary.passed}</div>
            </div>
            <div class="summary-card">
                <h3>Failed</h3>
                <div class="value" style="color: #ff6b6b;">${data.summary.failed}</div>
            </div>
            <div class="summary-card">
                <h3>Pass Rate</h3>
                <div class="value">${data.summary.passRate.toFixed(1)}%</div>
            </div>
        </div>

        <div class="test-suites">
            <h2>Test Results by Suite</h2>
            ${data.testSuites
              .map(
                (suite) => `
            <div class="test-suite">
                <h2>${suite.name}</h2>
                <div class="test-suite-stats">
                    <div class="stat">
                        <div class="label">Total</div>
                        <div class="value">${suite.total}</div>
                    </div>
                    <div class="stat">
                        <div class="label">Passed</div>
                        <div class="value" style="color: #51cf66;">${suite.passed}</div>
                    </div>
                    <div class="stat">
                        <div class="label">Failed</div>
                        <div class="value" style="color: #ff6b6b;">${suite.failed}</div>
                    </div>
                    <div class="stat">
                        <div class="label">Duration</div>
                        <div class="value">${(suite.duration / 1000).toFixed(2)}s</div>
                    </div>
                </div>
            </div>
            `,
              )
              .join('')}
        </div>

        ${
          data.failingTests.length > 0
            ? `
        <div class="failed-tests">
            <h2>Failed Tests</h2>
            ${data.failingTests
              .map(
                (failure) => `
            <div class="failed-test">
                <h3>${failure.name}</h3>
                <p><strong>Suite:</strong> ${failure.suite}</p>
                <p><strong>Module:</strong> ${failure.module}</p>
                <p><strong>Severity:</strong> ${failure.severity.toUpperCase()}</p>
                <div class="error">${failure.error}</div>
            </div>
            `,
              )
              .join('')}
        </div>
        `
            : ''
        }

        ${
          data.criticalTodos.length > 0
            ? `
        <div class="todos">
            <div class="todo-section">
                <h2>🔴 Critical Todos</h2>
                ${data.criticalTodos
                  .map(
                    (todo) => `
                <div class="todo-item todo-critical">
                    <div class="todo-title">
                        <input type="checkbox" /> ${todo.title}
                    </div>
                    <div class="todo-meta">
                        <span><strong>Module:</strong> ${todo.module}</span>
                        <span><strong>Deadline:</strong> ${todo.deadline}</span>
                        <span><strong>Blocking:</strong> ${todo.isBlocking ? 'Yes' : 'No'}</span>
                    </div>
                </div>
                `,
                  )
                  .join('')}
            </div>
        </div>
        `
            : ''
        }

        <footer>
            <p>Generated by Test Reporting System | ${new Date().toLocaleString()}</p>
        </footer>
    </div>
</body>
</html>`;

    fs.writeFileSync(filePath, html);
    console.log(`✓ HTML report created: ${filePath}`);

    return filePath;
  }

  /**
   * Generate all reports
   */
  generateAllReports(): { json: string; markdown: string; html: string; todos: string } {
    return {
      json: this.generateJsonReport(),
      markdown: this.generateMarkdownReport(),
      html: this.generateHtmlReport(),
      todos: this.generateTodoReport(),
    };
  }

  /**
   * Generate summary file
   */
  generateSummary(): string {
    const data = this.generateReportData();
    const filePath = path.join(this.reportDir, 'latest-summary.md');

    let markdown = `# Latest Test Execution Summary\n\n`;
    markdown += `**Last Updated**: ${new Date(data.timestamp).toLocaleString()}\n\n`;

    markdown += `## 📊 Quick Stats\n\n`;
    markdown += `| Metric | Value |\n`;
    markdown += `|--------|-------|\n`;
    markdown += `| Total Tests | ${data.summary.totalTests} |\n`;
    markdown += `| Pass Rate | ${data.summary.passRate.toFixed(2)}% |\n`;
    markdown += `| Failed | ${data.summary.failed} |\n`;
    markdown += `| Critical Todos | ${data.criticalTodos.length} |\n\n`;

    markdown += `## 📁 Available Reports\n\n`;
    markdown += `- [Full Markdown Report](${this.reportId}.md)\n`;
    markdown += `- [JSON Report](${this.reportId}.json)\n`;
    markdown += `- [HTML Report](${this.reportId}.html)\n`;
    markdown += `- [TODO Report](${this.reportId}-todos.md)\n`;

    fs.writeFileSync(filePath, markdown);
    return filePath;
  }
}

// Export for use in Jest configuration
export default TestReporter;
