name: "Edge Gateway: Externalized Config"
about: "Integrate Spring Cloud Config for dynamic and secure configuration management."
title: "Integrate Externalized Configuration for Edge Gateway"
labels: [enhancement, config, gateway]
---

## Problem
The Edge Gateway does not use externalized configuration (e.g., Spring Cloud Config). This limits dynamic updates and secure management of secrets.

## Solution
- Integrate Spring Cloud Config with the Edge Gateway.
- Move sensitive and environment-specific settings to the config server.
- Document how to update configuration dynamically.

## Acceptance Criteria
- Gateway loads configuration from a central config server.
- Secrets and environment variables are not hardcoded.
- Configuration changes can be applied without redeploying the gateway.

---
