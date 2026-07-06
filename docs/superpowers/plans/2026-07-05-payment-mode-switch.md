# Payment Mode Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a mutually exclusive admin payment mode switch for manual QR transfer and WeChat online payment.

**Architecture:** Add common payment mode constants/utilities, a backend `PaymentModeService`, admin config endpoints, and a small admin UI panel. Existing order payment and pre-order config endpoints read the active mode to expose compatible payment options.

**Tech Stack:** Java 8, Spring Boot, MyBatis Plus, Vue 2, Element UI, uni-app.

---

### Task 1: Common Payment Mode Contract

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/PaymentModeConstants.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/utils/PaymentModeUtil.java`
- Test: `crmeb/crmeb-common/src/test/java/com/zbkj/common/utils/PaymentModeUtilTest.java`

- [ ] Write failing tests for valid modes and legacy fallback.
- [ ] Implement constants and utility normalization.
- [ ] Run `mvn -pl crmeb-common test`.

### Task 2: Backend Mode Service And API

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/PaymentModeSwitchRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/PaymentModeResponse.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/PaymentModeService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/PaymentModeServiceImpl.java`
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SystemConfigController.java`

- [ ] Write failing service tests for mutual exclusion and validation.
- [ ] Implement service and controller.
- [ ] Run service/admin tests.

### Task 3: Runtime Payment Compatibility

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderPayServiceImpl.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`
- Modify: `app/store/modules/app.js`
- Modify: `app/pages/order/order_payment/index.vue`
- Modify: `app/pages/order/order_confirm/index.vue`
- Modify: `app/components/payment/index.vue`

- [ ] Restore WeChat online payment branch when mode is `wechat_online`.
- [ ] Keep manual QR branch active only when mode is `offline_qr`.
- [ ] Return mutually exclusive payment config to mobile frontend.

### Task 4: Admin UI

**Files:**
- Modify: `admin/src/api/systemConfig.js`
- Modify: `admin/src/views/systemSetting/setting/index.vue`

- [ ] Add API wrappers.
- [ ] Add payment mode panel with mutually exclusive switch buttons.
- [ ] Reload current mode after switching.

### Task 5: Verification

**Files:**
- Run tests/builds only.

- [ ] Run backend unit tests.
- [ ] Run backend package or compile.
- [ ] Run admin build.
- [ ] Run app build if the project script supports it.
- [ ] Run route/API smoke checks with local text search and compile output.
