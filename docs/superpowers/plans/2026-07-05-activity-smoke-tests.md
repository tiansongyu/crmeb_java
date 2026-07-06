# Activity Smoke Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add repeatable smoke and regression coverage for group-buy and bargain activity SKU paths so missing activity specs are caught before users see broken activity pages.

**Architecture:** Service tests cover the historical missing-SKU failure branches without a database. A standalone smoke script checks the running front API and validates active database activity SKU integrity against the linked master product SKUs.

**Tech Stack:** Java 8, JUnit 4, Mockito, Maven, Python 3 standard library, Docker MySQL client.

---

### Task 1: Service Regression Tests

**Files:**
- Modify: `crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/StoreCombinationServiceImplTest.java`
- Create: `crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/StoreBargainServiceImplTest.java`

- [ ] **Step 1: Add failing tests**

Add tests for:
- `StoreCombinationServiceImpl.getH5Detail()` returns master product attrs when combination activity attrs are missing.
- `StoreBargainServiceImpl.getH5Detail()` returns master product SKU when bargain activity SKU is missing.

- [ ] **Step 2: Verify RED**

Run:

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn -pl crmeb-service -am -Dtest=StoreCombinationServiceImplTest,StoreBargainServiceImplTest -DfailIfNoTests=false test -q
```

Expected before implementation:
- Combination detail fallback test fails because `productAttr` is empty.
- Bargain detail fallback test fails with `CrmebException: 砍价商品规格属性值未找到`.

### Task 2: Minimal Activity SKU Compatibility

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreCombinationServiceImpl.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreBargainServiceImpl.java`

- [ ] **Step 1: Implement group-buy detail fallback**

Replace direct activity attr lookup in `getH5Detail()` with the existing helper:

```java
List<StoreProductAttr> attrList = getCombinationSkuAttrList(storeCombination);
```

- [ ] **Step 2: Implement bargain detail fallback**

When bargain activity SKU values are empty, use the first linked master product SKU to populate `attrValueId` and `sku` for the detail response.

- [ ] **Step 3: Verify GREEN**

Run the same Maven test command from Task 1 and require all tests to pass.

### Task 3: API and Database Smoke Script

**Files:**
- Create: `scripts/smoke/activity_smoke.py`

- [ ] **Step 1: Add script**

The script must:
- Log in to the front API with `FRONT_ACCOUNT` and `FRONT_PASSWORD`.
- Call group-buy `index`, `header`, `list`, `detail/{id}`, `more`, and `pink/{pinkId}` when data exists.
- Call bargain `index`, `header`, `list`, and `detail/{id}` when data exists.
- Query MySQL through `docker exec crmeb-mysql mysql`.
- Fail if any active group-buy activity has no activity attrs, no activity SKU values, no master SKU values, or activity SKUs not present in master SKUs.
- Fail if any active bargain activity has no activity SKU values, no master SKU values, or activity SKUs not present in master SKUs.

- [ ] **Step 2: Run script against the local test front service**

Run:

```bash
BASE_URL=http://127.0.0.1:21410 \
DB_NAME=crmeb_payment_test_20260705120436 \
FRONT_ACCOUNT=<test-account> \
FRONT_PASSWORD=<test-password> \
python3 scripts/smoke/activity_smoke.py
```

Expected:
- Exit code `0`.
- Report every checked endpoint and database integrity rule.

### Task 4: Build Verification

**Files:**
- No source changes.

- [ ] **Step 1: Compile front and admin backends**

Run:

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn -pl crmeb-admin,crmeb-front -am -DskipTests compile -q
```

- [ ] **Step 2: Check patch cleanliness**

Run:

```bash
git diff --check -- crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreCombinationServiceImpl.java crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreBargainServiceImpl.java crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/StoreCombinationServiceImplTest.java crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/StoreBargainServiceImplTest.java scripts/smoke/activity_smoke.py
```

Expected: no output and exit code `0`.
