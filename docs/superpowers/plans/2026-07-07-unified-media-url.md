# Unified Media URL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align admin, H5, and backend media URL normalization so uploaded images and files render consistently across thumbnails, products, payment proof screenshots, QR codes, and rich text content.

**Architecture:** Keep the existing utility entry points and request interceptors. Expand their shared behavior through tests first, then make minimal utility changes so all page-level consumers benefit without broad view rewrites.

**Tech Stack:** Vue 2 admin, uni-app H5, Node-based unit tests, Java Spring Boot services, Docker-based verification.

---

### Task 1: Admin Media URL Utility

**Files:**
- Modify: `admin/tests/unit/utils/mediaUrl.spec.js`
- Modify: `admin/src/utils/mediaUrl.js`

- [ ] **Step 1: Write failing admin tests**

Add assertions that admin normalization strips `10.x` and `172.16-31.x` private hosts, preserves public CDN URLs, preserves `/__image/...`, and normalizes CSS `url(undefined/crmebimage/...)`.

- [ ] **Step 2: Verify admin tests fail**

Run with Docker:

```bash
docker run --rm -v "$PWD/admin:/workspace" -w /workspace node:14-bullseye sh -lc 'if [ ! -d node_modules ]; then npm ci --legacy-peer-deps; fi; npx vue-cli-service test:unit tests/unit/utils/mediaUrl.spec.js --runInBand'
```

Expected: the new assertions fail before implementation.

- [ ] **Step 3: Implement minimal admin normalization changes**

Update `admin/src/utils/mediaUrl.js` so the private-host and `undefined/` media cases match the design.

- [ ] **Step 4: Verify admin tests pass**

Run the same Docker command. Expected: the media URL test suite passes.

### Task 2: H5 Media URL Utility

**Files:**
- Modify: `app/tests/image-url.test.cjs`
- Modify: `app/utils/imageUrl.js`

- [ ] **Step 1: Write failing H5 tests**

Add assertions for `undefined/crmebimage/...`, `blob:` preservation, `10.x` and `172.16-31.x` private hosts, public CDN preservation, and nested HTML/CSS normalization.

- [ ] **Step 2: Verify H5 tests fail**

Run with Docker:

```bash
docker run --rm -v "$PWD/app:/workspace" -w /workspace node:14-bullseye node tests/image-url.test.cjs
```

Expected: at least one new assertion fails before implementation.

- [ ] **Step 3: Implement minimal H5 normalization changes**

Update `app/utils/imageUrl.js` so it shares the same media matching and private-host behavior as the admin utility while preserving the H5 image-host selection logic.

- [ ] **Step 4: Verify H5 tests pass**

Run the same Docker command. Expected: `image-url normalization checks passed`.

### Task 3: Backend Prefix Coverage

**Files:**
- Modify: `crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/SystemAttachmentServiceImplTest.java` or create it if absent
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SystemAttachmentServiceImpl.java`

- [ ] **Step 1: Write failing backend tests**

Add tests for `prefixImage`/`prefixUploadf` covering private URL stripping, public URL preservation, `undefined/crmebimage/...`, and blank values.

- [ ] **Step 2: Verify backend tests fail**

Run with Docker:

```bash
docker run --rm -v crmeb-maven-cache:/root/.m2 -v "$PWD/crmeb:/workspace" -w /workspace maven:3.8.8-eclipse-temurin-8 mvn -pl crmeb-common,crmeb-service -Dtest=SystemAttachmentServiceImplTest -DfailIfNoTests=false test
```

Expected: new assertions expose any behavior mismatch before implementation.

- [ ] **Step 3: Implement minimal backend prefix changes**

Update only `SystemAttachmentServiceImpl` helpers if the tests show mismatches.

- [ ] **Step 4: Verify backend tests pass**

Run the same Docker command. Expected: backend tests pass.

### Task 4: Final Verification

**Files:**
- No new production files expected beyond the utilities above.

- [ ] **Step 1: Run focused Docker verification**

```bash
docker run --rm -v "$PWD/admin:/workspace" -w /workspace node:14-bullseye sh -lc 'if [ ! -d node_modules ]; then npm ci --legacy-peer-deps; fi; npx vue-cli-service test:unit tests/unit/utils/mediaUrl.spec.js --runInBand'
docker run --rm -v "$PWD/app:/workspace" -w /workspace node:14-bullseye node tests/image-url.test.cjs
docker run --rm -v crmeb-maven-cache:/root/.m2 -v "$PWD/crmeb:/workspace" -w /workspace maven:3.8.8-eclipse-temurin-8 mvn -pl crmeb-common,crmeb-service -Dtest=SystemAttachmentServiceImplTest -DfailIfNoTests=false test
```

- [ ] **Step 2: Run compile verification**

```bash
docker run --rm -v crmeb-maven-cache:/root/.m2 -v "$PWD/crmeb:/workspace" -w /workspace maven:3.8.8-eclipse-temurin-8 mvn -pl crmeb-common,crmeb-service,crmeb-admin,crmeb-front -DskipTests compile
```

- [ ] **Step 3: Review git diff**

Confirm the change is limited to media normalization utilities, tests, and planning docs.
