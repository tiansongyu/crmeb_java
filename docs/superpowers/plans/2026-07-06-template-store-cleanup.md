# Template Store Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the project seed data and runtime presentation from a demo shop into a neutral template shop with placeholder categories and no real business data.

**Architecture:** Add an idempotent SQL cleanup/seed migration after the existing base SQL imports. Keep required platform tables intact, clear demo/business tables, add neutral placeholder categories and a default DIY page, and replace runtime-visible branding/configuration with template values.

**Tech Stack:** MySQL 5.7 SQL, Docker Compose, Spring Boot YAML resources, Vue/uni-app static HTML metadata, existing Docker-only verification script.

---

### Task 1: Add Template Cleanup SQL

**Files:**
- Create: `crmeb/sql/template_store_cleanup_20260706.sql`
- Modify: `docker-compose.yml`

- [ ] Create an idempotent SQL migration that disables foreign-key checks, clears demo/business tables, inserts template categories, inserts a lightweight default DIY homepage, updates system config template values, keeps manual QR payment mode enabled, and re-enables foreign-key checks.
- [ ] Add the SQL file to the MySQL init order in `docker-compose.yml` after activity SKU repair.
- [ ] Verify by importing the init SQL set into a disposable MySQL container.

### Task 2: Replace Runtime Branding

**Files:**
- Modify: `admin/public/index.html`
- Modify: `app/manifest.json`
- Modify: `app/pages.json`
- Modify: `crmeb/crmeb-admin/src/main/resources/application.yml`
- Modify: `crmeb/crmeb-admin/src/main/resources/application-dev.yml`
- Modify: `crmeb/crmeb-admin/src/main/resources/application-prod.yml`
- Modify: `crmeb/crmeb-admin/src/main/resources/application-trip.yml`
- Modify: `crmeb/crmeb-front/src/main/resources/application.yml`
- Modify: `crmeb/crmeb-front/src/main/resources/application-dev.yml`
- Modify: `crmeb/crmeb-front/src/main/resources/application-prod.yml`
- Modify: `crmeb/crmeb-front/src/main/resources/application-trip.yml`
- Modify: `crmeb/crmeb-admin/src/main/resources/banner.txt`
- Modify: `crmeb/crmeb-front/src/main/resources/banner.txt`

- [ ] Replace visible names and metadata with `商城模板` / `Template Store`.
- [ ] Set `demoSite: false` in admin and front configs.
- [ ] Replace CAPTCHA watermark with `Template Store`.
- [ ] Remove demo-site URLs from startup banners.

### Task 3: Disable External Branding/Telemetry Defaults

**Files:**
- Modify: `crmeb/crmeb-admin/src/main/resources/application*.yml`
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/config/StartupRunner.java`

- [ ] Set `asyncConfig: false` in default configs where the app previously synced seeded config automatically.
- [ ] Guard the legacy remote startup update call so it is disabled unless explicitly configured.

### Task 4: Update Tests and Smoke Defaults

**Files:**
- Modify: `crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/OrderServiceImplTest.java`
- Modify: `scripts/smoke/combination_sku_popup_smoke.py`
- Create: `scripts/docker/verify-template-sql.sh`

- [ ] Replace demo product names in tests with neutral fixture names.
- [ ] Make the old combination SKU smoke script require explicit IDs/SKU when a demo dataset is no longer present.
- [ ] Add Docker/MySQL SQL verification script for template cleanup assertions.

### Task 5: Docker Verification

**Files:**
- Modify if needed: `scripts/docker/verify-builds.sh`

- [ ] Run `scripts/docker/verify-template-sql.sh`.
- [ ] Run `TAG_SUFFIX=template scripts/docker/verify-builds.sh`.
- [ ] Record any remaining non-template display references that are intentionally technical identifiers.

### Task 6: Commit

**Files:**
- All files above.

- [ ] Run `git diff --check`.
- [ ] Commit with message `feat: convert demo data to template store seed`.
