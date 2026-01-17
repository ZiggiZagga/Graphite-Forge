/**
 * Jest Custom Reporter - Integrates with Test Reporting System
 * Captures test results and feeds them into structured reporting
 */

import { AggregatedResult, TestResult, AssertionResult } from '@jest/test-result';
import * as path from 'path';
import TestReporter, { TestSuiteResult, TestFailure } from './test-reporter';

class TestReportingJestReporter {
  private reporter: TestReporter;
  private globalConfig: any;

  constructor(globalConfig: any) {
    this.globalConfig = globalConfig;
    this.reporter = new TestReporter();
  }

  /**
   * Called after all tests have finished
   */
  onTestResult(
    test: any,
    testResult: TestResult,
    aggregatedResults: AggregatedResult,
  ): void | Promise<void> {
    const failedTests: TestFailure[] = [];

    // Extract failures
    if (testResult.testResults && testResult.testResults.length > 0) {
      testResult.testResults.forEach((assertion: AssertionResult) => {
        if (assertion.status === 'failed') {
          const failureMessage = assertion.failureMessages
            ? assertion.failureMessages.join('\n')
            : 'Unknown error';

          failedTests.push({
            name: assertion.title,
            suite: testResult.displayName || 'Unknown Suite',
            error: failureMessage.split('\n')[0], // First line of error
            stack: failureMessage,
            severity: this.determineSeverity(assertion.title, failureMessage),
            module: this.extractModule(testResult.testFilePath),
            isBlocking: this.isBlockingTest(assertion.title),
          });
        }
      });
    }

    // Add to reporter
    const suiteResult: TestSuiteResult = {
      name: testResult.displayName || path.basename(testResult.testFilePath),
      path: testResult.testFilePath,
      total: testResult.numTotalTests,
      passed: testResult.numPassingTests,
      failed: testResult.numFailingTests,
      skipped: testResult.numPendingTests,
      duration: testResult.perfStats?.end - testResult.perfStats?.start || 0,
      tests: [],
      failedTests,
    };

    this.reporter.addTestSuite(suiteResult);
  }

  /**
   * Called at the end of test run
   */
  onRunComplete(): void | Promise<void> {
    console.log('\n📊 Generating test reports...\n');
    this.reporter.generateAllReports();
    this.reporter.generateSummary();
    console.log('\n✅ All reports generated in ./test-results/reports/\n');
  }

  /**
   * Determine severity based on test name and error
   */
  private determineSeverity(testName: string, error: string): 'critical' | 'high' | 'medium' | 'low' {
    const criticalKeywords = ['auth', 'security', 'payment', 'database', 'api'];
    const testLower = testName.toLowerCase();
    const errorLower = error.toLowerCase();

    for (const keyword of criticalKeywords) {
      if (testLower.includes(keyword) || errorLower.includes(keyword)) {
        return 'critical';
      }
    }

    if (testLower.includes('integration') || errorLower.includes('timeout')) {
      return 'high';
    }

    if (testLower.includes('unit') || errorLower.includes('assertion')) {
      return 'medium';
    }

    return 'low';
  }

  /**
   * Extract module name from file path
   */
  private extractModule(filePath: string): string {
    const parts = filePath.split(path.sep);
    // Look for common module paths
    const moduleIndex = parts.findIndex((p) => ['src', 'tests', 'lib'].includes(p));
    if (moduleIndex !== -1 && moduleIndex < parts.length - 1) {
      return parts[moduleIndex + 1];
    }
    return parts[parts.length - 2] || 'unknown';
  }

  /**
   * Determine if test is blocking
   */
  private isBlockingTest(testName: string): boolean {
    const blockingKeywords = ['must', 'required', 'critical', 'blocking'];
    return blockingKeywords.some((keyword) => testName.toLowerCase().includes(keyword));
  }
}

export = TestReportingJestReporter;
