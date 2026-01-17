# Contributing to Graphite-Forge

Thank you for your interest in contributing! This guide will help you get started.

## Getting Started

### 1. Fork & Clone
```bash
git clone https://github.com/ZiggiZagga/Graphite-Forge.git
cd Graphite-Forge
git checkout develop
```

### 2. Setup Local Environment
```bash
# Start all services
./scripts/spinup.sh

# Verify services are healthy
curl http://localhost:8083/actuator/health

# Setup test users (alice/bob)
bash scripts/setup-keycloak-dev-users.sh
```

### 3. Make Your Changes
- Create a feature branch: `git checkout -b feature/your-feature`
- Make focused, atomic commits
- Follow the coding standards below

### 4. Test Your Changes

**Recommended: Use comprehensive test reporter**
```bash
# Run all tests with automatic reporting and todo generation
./scripts/comprehensive-test-reporter.sh --all

# Check results
cat test-results/reports/LATEST-SUMMARY.md
```

**Traditional approach (if needed):**
```bash
# Unit tests
cd graphql-service && mvn test
cd ../ui && npm test

# E2E tests
./scripts/test-e2e.sh
```

💡 **The comprehensive reporter:**
- Runs all tests automatically
- Converts failures to structured todos
- Assigns severity and deadlines
- Generates 4 report formats
- See [Quick Reference](docs/TEST-REPORTING-QUICK-REFERENCE.md) for details

### 5. Submit a Pull Request
- Push to your fork
- Open a PR against `develop` branch
- Include a clear description of changes
- Link any related issues

## Development Workflow

### Code Organization

**GraphQL Service** (`/graphql-service`)
```
src/main/java/com/example/graphql/
├── controller/      # GraphQL endpoints
├── service/         # Business logic
├── repository/      # Data access
├── model/           # Domain entities (records)
├── config/          # Spring configuration
└── exception/       # Custom exceptions
```

**UI** (`/ui`)
```
├── app/             # Next.js app directory
│   ├── layout.tsx   # Root layout
│   └── page.tsx     # Homepage
├── lib/
│   └── apollo.tsx   # Apollo Client setup
└── styles/          # Global styles
```

### Coding Standards

#### Java
- Use **records** for immutable domain entities
- Follow **Spring Boot conventions**
- Use **pattern matching** for null checks
- Add **Javadoc** to public methods
- Prefix test methods with `test`

```java
// ✅ Good
public record BucketConfig(String name, String owner) {}

public Optional<Bucket> getBucket(String id) {
    return bucketRepository.findById(id);
}

// ❌ Avoid
public class BucketConfig {
    private String name;
    private String owner;
    // ... getters, setters
}
```

#### TypeScript/React
- Use **functional components** with hooks
- Add **PropTypes** or TypeScript interfaces
- Use **meaningful variable names**
- Follow **Next.js conventions**

```typescript
// ✅ Good
interface BucketProps {
  id: string;
  name: string;
}

export function BucketCard({ id, name }: BucketProps) {
  return <div>{name}</div>;
}

// ❌ Avoid
export function BucketCard(props: any) {
  return <div>{props.n}</div>;
}
```

### Commit Messages

Use clear, descriptive commit messages:

```
feat: Add bucket deletion with cascade delete

- Implement DELETE mutation in GraphQL schema
- Add BucketService.deleteBucket() method
- Update tests to verify cascade behavior

Closes #123
```

**Format**:
```
<type>: <subject>

<body>

<footer>
```

**Types**:
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code restructuring (no behavior change)
- `test` - Add or update tests
- `docs` - Documentation changes
- `chore` - Build, dependencies, tooling

## Testing

### Comprehensive Test Reporter (Recommended)

```bash
# Run all tests with automatic reporting
./scripts/comprehensive-test-reporter.sh --all

# Run specific test types
./scripts/comprehensive-test-reporter.sh --backend    # Maven tests only
./scripts/comprehensive-test-reporter.sh --e2e        # E2E tests only
./scripts/comprehensive-test-reporter.sh --roadmap    # Roadmap validation only

# Verbose output for debugging
./scripts/comprehensive-test-reporter.sh --all --verbose
```

**What you get:**
- ✅ All tests executed in correct order
- ✅ Failures converted to structured todos
- ✅ Severity-based prioritization (Critical/High/Medium/Low)
- ✅ Auto-assigned deadlines based on severity
- ✅ 4 report formats: Markdown, JSON, HTML, Summary

**View results:**
```bash
# Quick overview
cat test-results/reports/LATEST-SUMMARY.md

# Todo list with priorities
cat test-results/reports/*-todos.md

# Full detailed report
cat test-results/reports/test-report-*.md
```

📖 **Learn more:** [Test Reporting System Docs](docs/TEST-REPORTING-SYSTEM.md)

### Traditional Testing (Individual Commands)

#### Unit Tests (GraphQL Service)
```bash
cd graphql-service
mvn test

# Run specific test class
mvn test -Dtest=BucketServiceTest

# Run with coverage
mvn clean test jacoco:report
```

#### E2E Tests
```bash
# Run all E2E tests in container
./scripts/test-e2e.sh

# View detailed output
./scripts/test-e2e.sh --verbose
```

### Test Coverage Requirements
- New features: Minimum 80% code coverage
- Bug fixes: Add tests that reproduce the bug
- Refactoring: Maintain existing test coverage

### Todo-Driven Development

When tests fail, the comprehensive reporter generates todos:

```markdown
## 🔴 CRITICAL - Due: Same Day
- **Module:** graphql-service
- **Test:** PolicyManagementServiceTest
- **Issue:** WebClient Builder is null
- **Action:** Fix dependency injection for WebClient
```

**Workflow:**
1. Run tests: `./scripts/comprehensive-test-reporter.sh --all`
2. Review todos: `cat test-results/reports/*-todos.md`
3. Fix issues starting with CRITICAL priority
4. Re-run tests to verify fixes
5. Todos automatically disappear when tests pass

## Pull Request Process

### Before Submitting
- [ ] Tests pass: `./scripts/comprehensive-test-reporter.sh --all`
- [ ] All CRITICAL and HIGH priority todos resolved
- [ ] Check test results: `cat test-results/reports/LATEST-SUMMARY.md`
- [ ] Code follows style guidelines
- [ ] Added/updated tests as needed
- [ ] Updated documentation if needed
- [ ] No merge conflicts with `develop`

### PR Checklist
- [ ] Clear description of changes
- [ ] References related issues (`Closes #123`)
- [ ] All tests pass in CI/CD
- [ ] Code review approved
- [ ] Squash commits if needed

### Review Process
1. **Automated Checks**: CI/CD runs tests and linting
2. **Code Review**: Project maintainers review changes
3. **Feedback**: Iterate on feedback from reviewers
4. **Merge**: Once approved, changes are merged to `develop`

## Architecture Guidelines

### When to Add a New Service
- Complex domain logic that's isolated from other services
- Clear API contract between services
- Independent scaling requirements

### When to Modify GraphQL Schema
- Breaking changes require major version bump
- Document changes in schema comments
- Update frontend and tests simultaneously

### Configuration Management
- Environment-specific config in `application-{env}.yml`
- Sensitive data in Docker secrets (production)
- Logging configuration in `application.yml`

## Documentation

### Code Comments
Add comments for:
- **Why** (not what) code does something non-obvious
- Complex algorithms or business logic
- Workarounds for known issues

```java
// ✅ Good - explains WHY
// We use indexOf() instead of contains() because string comparison
// is case-insensitive in MinIO but case-sensitive in our DB
int index = bucketName.toLowerCase().indexOf(query);

// ❌ Avoid - obvious what code does
// Find the bucket name
int index = bucketName.indexOf(query);
```

### Documentation Updates
Update docs when:
- Adding new features
- Changing API contracts
- Modifying deployment procedures
- Adding new dependencies

## Reporting Issues

### Creating an Issue
Include:
- **Clear title** describing the problem
- **Steps to reproduce** (if bug)
- **Expected behavior** vs. actual behavior
- **System information** (OS, Docker version, etc.)
- **Relevant logs** or error messages

### Bug Report Example
```markdown
## Bug: GraphQL query fails with invalid token

### Steps to Reproduce
1. Login with alice user
2. Wait 5 minutes for token to expire
3. Execute any GraphQL query
4. Observe error

### Expected
Query should fail with clear "Unauthorized" error

### Actual
Query fails with cryptic "NPE in TokenValidator"

### Logs
[paste error logs here]
```

## Setting Up for Development

### IDE Setup

**Visual Studio Code**
- Extensions:
  - Extension Pack for Java
  - GraphQL: Language Feature Support
  - Prettier (formatter)
  - ESLint
  - REST Client

**IntelliJ IDEA**
- Install GraphQL plugin
- Enable code inspections
- Configure code style

### Build & Run
```bash
# Build all services
./scripts/spinup.sh --rebuild

# Run specific service
cd graphql-service && mvn spring-boot:run

# Watch for changes
cd ui && npm run dev
```

## Performance Tips

### GraphQL Optimization
- Use field selection to avoid over-fetching
- Batch queries when possible
- Cache results on frontend

### Database Queries
- Add indexes on frequently queried columns
- Use `@Query` with specific fields
- Profile slow queries with EXPLAIN

### Frontend Optimization
- Code split components
- Lazy load images
- Use React.memo for expensive components

## Getting Help

- 📖 [Architecture Guide](ARCHITECTURE.md) - System design
- 🚀 [Roadmap](ROADMAP.md) - Planned features
- 💬 [GitHub Discussions](https://github.com/ZiggiZagga/Graphite-Forge/discussions)
- 🐛 [GitHub Issues](https://github.com/ZiggiZagga/Graphite-Forge/issues)

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on code, not the person
- Help others succeed

---

**Happy coding! 🎉 Looking forward to your contributions!**
