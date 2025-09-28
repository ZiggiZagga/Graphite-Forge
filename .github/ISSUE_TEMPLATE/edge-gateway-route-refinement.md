name: "Edge Gateway: Route Refinement"
about: "Refine route predicates and filters to avoid accidental exposure and improve routing."
title: "Refine Route Predicates and Filters in Edge Gateway"
labels: [enhancement, gateway]
---

## Problem
Some route predicates in the Edge Gateway are too broad (e.g., `Path=/world/`), which may unintentionally match more than intended. The `RewritePath` filter for `/graphql/` may also need refinement.

## Solution
- Review and tighten all route predicates (e.g., use `/world/**` for subpaths).
- Ensure filters like `RewritePath` match backend expectations and do not introduce routing errors.
- Add comments to the YAML config explaining each route and filter.

## Acceptance Criteria
- All routes match only the intended paths.
- Filters are correct and documented.
- No accidental exposure of endpoints.

---
