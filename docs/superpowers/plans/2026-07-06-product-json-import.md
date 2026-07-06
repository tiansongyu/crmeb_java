# Product JSON Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a JSON batch import flow that lets admins create many products quickly from one file.

**Architecture:** Add a focused backend product import service that parses uploaded JSON, converts each item to the existing `StoreProductAddRequest`, and delegates persistence to `StoreProductService.save`. Add an admin product-list dialog for template download, validation, and import. Extend template cleanup SQL with import permission and a default free-shipping template.

**Tech Stack:** Spring Boot multipart upload, Fastjson, MyBatis Plus services, Vue2 + ElementUI, Docker Maven/Node build verification.

---

### Task 1: Backend DTOs and Import Service

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ProductImportFileRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ProductImportItemRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ProductImportSkuRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ProductImportItemResponse.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ProductImportResponse.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/ProductImportService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/ProductImportServiceImpl.java`
- Test: `crmeb/crmeb-service/src/test/java/com/zbkj/service/service/impl/ProductImportServiceImplTest.java`

- [ ] Write unit tests for dry-run, successful import conversion, category auto-create, default shipping template creation, and per-row failures.
- [ ] Run the backend test in Docker and confirm the tests fail because the service does not exist.
- [ ] Implement DTOs and `ProductImportServiceImpl`.
- [ ] Run the backend test in Docker and confirm it passes.

### Task 2: Backend Controller and Permissions

**Files:**
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/StoreProductController.java`
- Modify: `crmeb/sql/template_store_cleanup_20260706.sql`
- Modify: `docker-compose.yml`

- [ ] Add `POST /api/admin/store/product/import/json`.
- [ ] Add `admin:product:import:json` permission to template cleanup SQL if missing.
- [ ] Insert a default free-shipping template in template cleanup SQL.
- [ ] Mount `template_store_cleanup_20260706.sql` into MySQL init order after activity SKU repair.

### Task 3: Admin UI Import Dialog

**Files:**
- Modify: `admin/src/api/store.js`
- Modify: `admin/src/views/store/index.vue`
- Create: `docs/templates/product-import-template.json`

- [ ] Add API client for JSON import.
- [ ] Add product-list `批量导入` button and dialog.
- [ ] Add local template JSON file.
- [ ] Validate selected JSON before enabling import.

### Task 4: Finish Template Store Cleanup

**Files:**
- Modify runtime branding/config files listed in `docs/superpowers/plans/2026-07-06-template-store-cleanup.md`
- Modify: `app/pages/index/index.vue`
- Create: `scripts/docker/verify-template-sql.sh`

- [ ] Replace visible demo branding with template wording.
- [ ] Add empty-home placeholder UI for template stores.
- [ ] Add Docker SQL verification assertions for cleanup SQL.

### Task 5: Docker Verification and Deploy

**Files:**
- Modify as needed: `scripts/docker/verify-builds.sh`

- [ ] Run `scripts/docker/verify-template-sql.sh`.
- [ ] Run Docker Maven tests including `ProductImportServiceImplTest`.
- [ ] Run `TAG_SUFFIX=template-import scripts/docker/verify-builds.sh`.
- [ ] Commit changes.
- [ ] Deploy to the SSH server using the repository Docker deployment flow.
