# Purchase Flow Smoke Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and run a repeatable smoke test that covers the template store's end-to-end purchase, manual QR payment, admin payment audit, shipment, receipt, refund approval, and refund refusal flows.

**Architecture:** Add one Python stdlib smoke runner under `tests/` that drives the deployed HTTP APIs with admin and front tokens. The runner creates fresh orders from public product data, uses manual QR payment for deterministic payment, uses fictitious shipment to avoid third-party logistics dependencies, and verifies order states through both front and admin APIs.

**Tech Stack:** Python 3 stdlib (`urllib`, `json`, `argparse`), front/admin REST APIs, existing Docker/manual-QR deployment.

---

### Task 1: Implement Purchase Flow Smoke Runner

**Files:**
- Create: `tests/purchase_flow_smoke.py`

- [ ] **Step 1: Write the failing smoke contract**

Run before the file exists:

```bash
python3 tests/purchase_flow_smoke.py --help
```

Expected: FAIL with `No such file or directory`.

- [ ] **Step 2: Create the smoke runner**

Create `tests/purchase_flow_smoke.py` with:

```python
#!/usr/bin/env python3
"""End-to-end template store purchase flow smoke tests."""
```

The script must:
- Login as admin and front user.
- Select an active public product and SKU.
- Ensure a front user address exists.
- Exercise: cart add/list/count/delete, pre-order, load pre-order, computed price, order create, pay config, payment mode selection, upload payment proof, submit proof, admin order list/detail, offline audit approve/reject, admin shipment, front receipt, refund apply info, refund apply, admin refund approve, admin refund refuse, front order data/list/detail, logistics endpoint.
- Use separate orders for approve+ship+refund-success, refund-refuse, offline-payment-reject, and cancel flows to avoid incompatible state transitions.
- Fail on non-200 HTTP responses, non-`code=200` JSON envelopes, missing identifiers, or state values that contradict the expected transition.

- [ ] **Step 3: Run syntax check**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
source = Path('tests/purchase_flow_smoke.py').read_text(encoding='utf-8')
compile(source, 'tests/purchase_flow_smoke.py', 'exec')
print('purchase flow smoke syntax passed')
PY
```

Expected: `purchase flow smoke syntax passed`.

### Task 2: Run Against Remote Manual QR Deployment

**Files:**
- Read: `tests/purchase_flow_smoke.py`
- Use remote: `/root/github/crmeb_java`

- [ ] **Step 1: Sync script to server**

Run:

```bash
rsync -av tests/purchase_flow_smoke.py server:/root/github/crmeb_java/tests/purchase_flow_smoke.py
```

Expected: one file transferred.

- [ ] **Step 2: Run remote smoke**

Run:

```bash
ssh server 'cd /root/github/crmeb_java && python3 tests/purchase_flow_smoke.py --admin-api http://127.0.0.1:7002 --front-api http://127.0.0.1:7003 --admin-account admin --admin-password 123456 --front-account 18800001001 --front-password Test123456 --timeout 25'
```

Expected: all checks pass. If any check fails, use systematic debugging to identify whether the failure is test-data setup, API contract mismatch, or a production bug.

### Task 3: Fix Root Causes Found By Smoke

**Files:**
- Modify only files implicated by the failing evidence.
- Test: `tests/purchase_flow_smoke.py`

- [ ] **Step 1: Reproduce the failing smoke step**

Run the smallest command or API call that reproduces the failure and record the response body.

- [ ] **Step 2: Trace the broken state**

Inspect the relevant controller, request object, service method, and database row to find where the incorrect state originates.

- [ ] **Step 3: Add or adjust test coverage**

Update `tests/purchase_flow_smoke.py` so the failing behavior is checked explicitly.

- [ ] **Step 4: Apply the minimal code or data migration fix**

Patch only the root-cause file or SQL migration needed by the observed failure.

- [ ] **Step 5: Rebuild/redeploy if server code changed**

Run the existing manual-QR Docker compose build/deploy commands used by this branch.

- [ ] **Step 6: Re-run full remote smoke**

Run the Task 2 remote smoke command again and require all checks to pass.

### Task 4: Final Verification

**Files:**
- Read: `tests/purchase_flow_smoke.py`
- Read: `tests/api_smoke.py`
- Read: `tests/image_display_smoke.py`

- [ ] **Step 1: Run local static checks**

Run:

```bash
git diff --check
python3 - <<'PY'
from pathlib import Path
for path in ['tests/purchase_flow_smoke.py', 'tests/api_smoke.py', 'tests/image_display_smoke.py']:
    source = Path(path).read_text(encoding='utf-8')
    compile(source, path, 'exec')
print('python syntax checks passed')
PY
```

Expected: no whitespace errors and `python syntax checks passed`.

- [ ] **Step 2: Run server smoke suite**

Run:

```bash
ssh server 'cd /root/github/crmeb_java && python3 tests/api_smoke.py --admin-web http://127.0.0.1:7000 --admin-api http://127.0.0.1:7002 --front-api http://127.0.0.1:7003 --timeout 20'
ssh server 'cd /root/github/crmeb_java && python3 tests/purchase_flow_smoke.py --admin-api http://127.0.0.1:7002 --front-api http://127.0.0.1:7003 --admin-account admin --admin-password 123456 --front-account 18800001001 --front-password Test123456 --timeout 25'
```

Expected: both smoke scripts pass.

- [ ] **Step 3: Report evidence**

Summarize the exact flows covered, commands run, pass/fail counts, and any intentionally excluded third-party-dependent behavior.
