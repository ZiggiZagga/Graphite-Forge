name: "Edge Gateway: Observability"
about: "Add distributed tracing and structured logging to the Edge Gateway."
title: "Add Observability to Edge Gateway (Tracing & Logging)"
labels: [enhancement, observability, gateway]
---

## Problem
The Edge Gateway currently lacks distributed tracing and structured logging. This makes it difficult to diagnose issues and trace requests across microservices.

## Solution
- Integrate distributed tracing (e.g., Spring Cloud Sleuth, Zipkin, or OpenTelemetry).
- Add structured logging and correlation IDs to all requests.
- Document how to view and analyze traces/logs.

## Acceptance Criteria
- All requests through the gateway are traceable end-to-end.
- Logs include correlation IDs and are structured for analysis.
- Observability setup is documented.

---
