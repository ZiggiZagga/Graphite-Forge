/**
 * Jest Setup File for Roadmap Tests
 * 
 * This file runs before all tests in the roadmap test suite.
 * It initializes test environment and sets up common utilities.
 */

// Set test timeout
jest.setTimeout(30000);

// Suppress console warnings during tests (optional)
const originalWarn = console.warn;
const originalError = console.error;

beforeAll(() => {
  // Optionally suppress specific warnings
  // console.warn = jest.fn();
  // console.error = jest.fn();
});

afterAll(() => {
  // Restore console
  console.warn = originalWarn;
  console.error = originalError;
});

// Custom matchers for roadmap testing
expect.extend({
  /**
   * Checks if a feature has valid priority
   */
  toHaveValidPriority(feature: any) {
    const validPriorities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    const pass = validPriorities.includes(feature.priority);

    if (pass) {
      return {
        message: () =>
          `expected feature ${feature.name} not to have valid priority ${feature.priority}`,
        pass: true,
      };
    } else {
      return {
        message: () =>
          `expected feature ${feature.name} to have valid priority, but got ${feature.priority}`,
        pass: false,
      };
    }
  },

  /**
   * Checks if a feature has reasonable effort estimate
   */
  toHaveReasonableEffort(feature: any) {
    const isReasonable = feature.estimatedDays > 0 && feature.estimatedDays <= 30;

    if (isReasonable) {
      return {
        message: () =>
          `expected feature ${feature.name} not to have reasonable effort ${feature.estimatedDays} days`,
        pass: true,
      };
    } else {
      return {
        message: () =>
          `expected feature ${feature.name} to have reasonable effort between 1-30 days, but got ${feature.estimatedDays}`,
        pass: false,
      };
    }
  },

  /**
   * Checks if a phase is complete
   */
  toBeCompletePhase(phase: any) {
    const completedFeatures = phase.features.filter((f: any) => f.status === 'completed');
    const completionRate = (completedFeatures.length / phase.features.length) * 100;
    const pass = completionRate >= 70;

    if (pass) {
      return {
        message: () => `expected phase ${phase.name} not to be complete (${completionRate}%)`,
        pass: true,
      };
    } else {
      return {
        message: () =>
          `expected phase ${phase.name} to be complete, but only ${completionRate}% of features are completed`,
        pass: false,
      };
    }
  },
});

// Test utilities
export const createMockFeature = (overrides = {}) => ({
  name: 'Test Feature',
  phase: 'Phase 1',
  priority: 'MEDIUM',
  estimatedDays: 5,
  status: 'planned',
  testCoverage: 0,
  dependencies: [],
  ...overrides,
});

export const createMockPhase = (overrides = {}) => ({
  name: 'Test Phase',
  quarter: 'Q1 2026',
  targetDate: '2026-03-31',
  features: [createMockFeature()],
  ...overrides,
});

// Global test utilities
global.testUtils = {
  createMockFeature,
  createMockPhase,
};

// Set up performance monitoring
const performanceMonitor = {
  testStartTime: 0,
  testName: '',
};

beforeEach(({ currentTest }: any) => {
  performanceMonitor.testStartTime = performance.now();
  performanceMonitor.testName = currentTest?.name || 'unknown';
});

afterEach(({ currentTest }: any) => {
  const duration = performance.now() - performanceMonitor.testStartTime;
  if (duration > 1000) {
    console.warn(
      `⚠️  Test "${performanceMonitor.testName}" took ${duration.toFixed(2)}ms (slow test)`,
    );
  }
});

// TypeScript type extensions
declare global {
  namespace jest {
    interface Matchers<R> {
      toHaveValidPriority(): R;
      toHaveReasonableEffort(): R;
      toBeCompletePhase(): R;
    }
  }
  var testUtils: {
    createMockFeature: (overrides?: any) => any;
    createMockPhase: (overrides?: any) => any;
  };
}

export {};
