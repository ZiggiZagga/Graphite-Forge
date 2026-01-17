/**
 * Jest Configuration for Roadmap Tests
 * 
 * This configuration is optimized for testing roadmap features,
 * specifications, and success criteria.
 */

module.exports = {
  displayName: 'roadmap-tests',
  testEnvironment: 'node',
  testMatch: ['**/tests/roadmap/**/*.test.ts'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: {
        target: 'ES2020',
        module: 'commonjs',
        lib: ['ES2020'],
        strict: true,
        esModuleInterop: true,
        skipLibCheck: true,
        forceConsistentCasingInFileNames: true,
        declaration: true,
        declarationMap: true,
        sourceMap: true,
      },
    }],
  },
  collectCoverageFrom: [
    'tests/roadmap/**/*.ts',
    '!tests/roadmap/**/*.test.ts',
    '!tests/roadmap/README.md',
    '!tests/roadmap/jest.config.js',
  ],
  coveragePathIgnorePatterns: [
    '/node_modules/',
    '/dist/',
  ],
  testTimeout: 30000,
  maxWorkers: '50%',
  verbose: true,
  bail: false,
  errorOnDeprecated: true,

  // Custom reporters for roadmap testing
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputDirectory: './test-results/roadmap',
        outputName: 'roadmap-test-results.xml',
        classNameTemplate: '{classname}',
        titleTemplate: '{title}',
        ancestorSeparator: ' › ',
        usePathAsClassName: true,
      },
    ],
    [
      'jest-html-reporter',
      {
        pageTitle: 'Graphite-Forge Roadmap Test Report',
        outputPath: './test-results/roadmap/index.html',
        includeFailureMsg: true,
        includeConsoleLog: true,
        theme: 'darkTheme',
      },
    ],
  ],

  // Setup files
  setupFilesAfterEnv: ['<rootDir>/tests/roadmap/setup.ts'],

  // Module name mapper for path aliases (if needed)
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/tests/$1',
  },

  // Coverage thresholds for roadmap tests
  coverageThresholdForRoadmapTests: {
    branches: 0, // Roadmap tests are specification, not implementation
    functions: 0,
    lines: 0,
    statements: 0,
  },

  // Performance thresholds
  slowTestThreshold: 5,

  // Clear mocks between tests
  clearMocks: true,
  restoreMocks: true,
  resetMocks: true,

  // Globals
  globals: {
    'ts-jest': {
      isolatedModules: true,
    },
  },
};
