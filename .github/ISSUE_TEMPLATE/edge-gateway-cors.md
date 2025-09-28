name: "Edge Gateway: CORS Policy"
about: "Define and enforce a CORS policy for the Edge Gateway."
title: "Add CORS Policy to Edge Gateway"
labels: [security, enhancement, gateway]
---

## Problem
The Edge Gateway does not define a CORS (Cross-Origin Resource Sharing) policy. This may block browser clients or expose the API to unwanted origins.

## Solution
- Add CORS configuration to the Edge Gateway.
- Allow only trusted origins (e.g., frontend domains).
- Document the CORS policy and how to update it.

## Acceptance Criteria
- CORS is enabled for trusted origins only.
- Preflight and actual requests from allowed origins succeed.
- Requests from untrusted origins are blocked.

---
