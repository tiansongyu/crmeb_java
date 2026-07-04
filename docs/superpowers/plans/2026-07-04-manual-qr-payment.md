# Manual QR Payment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a shopping-order payment flow that only supports merchant QR-code transfer, customer voucher upload, and admin approval or rejection.

**Architecture:** Reuse the existing `offline` pay type, add current voucher-review fields to `eb_store_order`, and reuse the existing paid-order follow-up queue after admin approval. Frontend payment options become a single manual-payment option backed by the extended pay config and order detail DTOs.

**Tech Stack:** Spring Boot 2.2, MyBatis Plus, MySQL, Redis queue, Vue 2 admin, uni-app customer app, JUnit, Node assert tests.

---

### Task 1: Shared Manual Payment Types And Tests

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/OfflinePayConstants.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/utils/OfflinePayUtil.java`
- Create: `crmeb/crmeb-common/src/test/java/com/zbkj/common/utils/OfflinePayUtilTest.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrder.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/PayConfigResponse.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/PreOrderResponse.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/OrderPayResultResponse.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/OrderDetailResponse.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreOrderDetailInfoResponse.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreOrderDetailResponse.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreOrderInfoResponse.java`

- [ ] **Step 1: Write failing tests**

```java
package com.zbkj.common.utils;

import com.zbkj.common.constants.OfflinePayConstants;
import org.junit.Test;

import static org.junit.Assert.*;

public class OfflinePayUtilTest {
    @Test
    public void labelsKnownStatuses() {
        assertEquals("未提交", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_NOT_SUBMITTED));
        assertEquals("待审核", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_PENDING));
        assertEquals("已通过", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_APPROVED));
        assertEquals("已驳回", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_REJECTED));
    }

    @Test
    public void detectsPendingReview() {
        assertTrue(OfflinePayUtil.isPending(OfflinePayConstants.STATUS_PENDING));
        assertFalse(OfflinePayUtil.isPending(OfflinePayConstants.STATUS_NOT_SUBMITTED));
        assertFalse(OfflinePayUtil.isPending(null));
    }

    @Test
    public void detectsRejectedReview() {
        assertTrue(OfflinePayUtil.isRejected(OfflinePayConstants.STATUS_REJECTED));
        assertFalse(OfflinePayUtil.isRejected(OfflinePayConstants.STATUS_PENDING));
        assertFalse(OfflinePayUtil.isRejected(null));
    }
}
```

- [ ] **Step 2: Verify red**

Run: `cd crmeb && ./mvnw -pl crmeb-common -Dtest=OfflinePayUtilTest test`

Expected: FAIL because `OfflinePayUtil` and `OfflinePayConstants` do not exist.

- [ ] **Step 3: Implement constants, helper, and DTO fields**

Add constants for manual payment config and status values, add helper label/state functions, and add the manual payment fields listed in the design doc to `StoreOrder` plus response DTOs.

- [ ] **Step 4: Verify green**

Run: `cd crmeb && ./mvnw -pl crmeb-common -Dtest=OfflinePayUtilTest test`

Expected: PASS.

### Task 2: Backend Manual Payment Service

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/OfflinePayProofRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/OfflinePayAuditRequest.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/OrderPayService.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderPayServiceImpl.java`
- Modify: `crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/PayController.java`
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/StoreOrderController.java`

- [ ] **Step 1: Write request objects**

`OfflinePayProofRequest` requires `orderNo` and `voucher`, with optional `tradeNo` and `remark`.

`OfflinePayAuditRequest` requires `orderNo` and `approved`; `reason` is required when `approved=false`.

- [ ] **Step 2: Implement service interface methods**

Add:

```java
OrderPayResultResponse submitOfflineProof(OfflinePayProofRequest request);

Boolean auditOfflinePay(OfflinePayAuditRequest request);
```

- [ ] **Step 3: Disable online payment methods**

In `getPayConfig`, force WeChat and balance payment to `false`, expose offline payment config fields, and ignore online config values for shopping order payment.

In `payment`, reject all `payType` values except `offline`. For `offline`, set order `payType=offline`, `isChannel=8`, `offlinePayStatus=0` if missing, keep `paid=false`, and return `payType=offline`.

- [ ] **Step 4: Implement voucher submission**

Validate current user ownership, unpaid status, enabled manual payment, and nonblank voucher URL. Update `offlinePayStatus=1`, voucher fields, submit time, and clear prior refusal.

- [ ] **Step 5: Implement admin audit**

Approve only pending vouchers. In one transaction, set `paid=true`, `payTime`, `offlinePayStatus=2`, audit fields, deduct used integral if needed, run combination-order setup logic, update order, and push `ORDER_TASK_PAY_SUCCESS_AFTER`.

Reject only pending vouchers. In one transaction, keep `paid=false`, set `offlinePayStatus=3`, refusal reason, audit fields, and write an order log.

- [ ] **Step 6: Expose endpoints**

Add `POST /api/front/pay/offline/proof` and `POST /api/admin/store/order/offline/audit`.

- [ ] **Step 7: Verify backend compile**

Run: `cd crmeb && ./mvnw -pl crmeb-front,crmeb-admin -am -DskipTests package`

Expected: SUCCESS.

### Task 3: Query, Status, SQL Migration

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreOrderServiceImpl.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StoreOrderSearchRequest.java`
- Create: `crmeb/sql/manual_qr_payment_20260704.sql`

- [ ] **Step 1: Include fields in admin list query**

Add the `offline_pay_*` columns to the selected admin order list fields so order list rows can show review state.

- [ ] **Step 2: Show manual-review status in H5 and admin**

Before normal unpaid status, return "付款凭证待审核" for `offlinePayStatus=1` and "付款凭证已驳回" for `offlinePayStatus=3`.

- [ ] **Step 3: Add admin status filter**

Allow `offlineReview` in `StoreOrderSearchRequest.status` and filter `pay_type='offline'`, `paid=false`, `offline_pay_status=1`.

- [ ] **Step 4: Add migration SQL**

Create SQL that adds the new order columns, updates the existing line-offline payment form template with QR-code fields, and inserts default config keys if missing.

- [ ] **Step 5: Apply migration to local Docker DB when available**

Run the SQL against the local MySQL container and tolerate duplicate-column errors by checking schema first.

### Task 4: Customer App Manual Payment UI

**Files:**
- Create: `app/utils/manualPayment.js`
- Create: `app/tests/manual-payment.test.cjs`
- Modify: `app/store/modules/app.js`
- Modify: `app/api/order.js`
- Modify: `app/components/payment/index.vue`
- Modify: `app/pages/order/order_payment/index.vue`
- Modify: `app/pages/order/order_pay_status/index.vue`
- Modify: `app/pages/order/order_details/index.vue`

- [ ] **Step 1: Write failing helper test**

```javascript
const assert = require('assert');
const {
  buildManualPayMode,
  getOfflineReviewLabel,
  canUploadOfflineVoucher,
} = require('../utils/manualPayment.js');

assert.deepStrictEqual(buildManualPayMode({ offlinePayStatus: true })[0].value, 'offline');
assert.strictEqual(buildManualPayMode({ offlinePayStatus: false }).length, 0);
assert.strictEqual(getOfflineReviewLabel(1), '付款凭证待审核');
assert.strictEqual(getOfflineReviewLabel(3), '付款凭证已驳回');
assert.strictEqual(canUploadOfflineVoucher({ paid: false, payType: 'offline', offlinePayStatus: 0 }), true);
assert.strictEqual(canUploadOfflineVoucher({ paid: false, payType: 'offline', offlinePayStatus: 3 }), true);
assert.strictEqual(canUploadOfflineVoucher({ paid: false, payType: 'offline', offlinePayStatus: 1 }), false);
assert.strictEqual(canUploadOfflineVoucher({ paid: true, payType: 'offline', offlinePayStatus: 2 }), false);

console.log('manual payment helper checks passed');
```

- [ ] **Step 2: Verify red**

Run: `node app/tests/manual-payment.test.cjs`

Expected: FAIL because `app/utils/manualPayment.js` does not exist.

- [ ] **Step 3: Implement helper and API wrapper**

Add `submitOfflinePayProof(data)` to `app/api/order.js`. Implement the helper functions used by payment pages.

- [ ] **Step 4: Replace payment options**

Make store payment config, popup payment component, and standalone order payment page show only the manual payment option.

- [ ] **Step 5: Add QR and upload UI**

Display configured QR code, amount, order number, optional serial input, optional note, voucher uploader, pending/rejected state text, and submit button.

- [ ] **Step 6: Verify helper green**

Run: `node app/tests/manual-payment.test.cjs`

Expected: PASS.

### Task 5: Admin Review UI

**Files:**
- Create: `admin/src/utils/offlinePay.js`
- Create: `admin/tests/unit/utils/offlinePay.spec.js`
- Modify: `admin/src/api/order.js`
- Modify: `admin/src/views/order/index.vue`
- Modify: `admin/src/views/order/orderDetail.vue`

- [ ] **Step 1: Write failing unit test**

```javascript
import { offlinePayStatusText, canAuditOfflinePay } from '@/utils/offlinePay';

describe('offlinePay utils', () => {
  it('labels statuses', () => {
    expect(offlinePayStatusText(0)).toBe('未提交');
    expect(offlinePayStatusText(1)).toBe('待审核');
    expect(offlinePayStatusText(2)).toBe('已通过');
    expect(offlinePayStatusText(3)).toBe('已驳回');
  });

  it('allows auditing only pending unpaid offline orders', () => {
    expect(canAuditOfflinePay({ payType: 'offline', paid: false, offlinePayStatus: 1 })).toBe(true);
    expect(canAuditOfflinePay({ payType: 'offline', paid: true, offlinePayStatus: 2 })).toBe(false);
    expect(canAuditOfflinePay({ payType: 'weixin', paid: false, offlinePayStatus: 1 })).toBe(false);
  });
});
```

- [ ] **Step 2: Verify red**

Run: `cd admin && npx vue-cli-service test:unit tests/unit/utils/offlinePay.spec.js`

Expected: FAIL because `@/utils/offlinePay` does not exist.

- [ ] **Step 3: Implement helper and API**

Add `offlinePayAuditApi(data)` for `/admin/store/order/offline/audit`.

- [ ] **Step 4: Update order list**

Add manual-payment status display, an `offlineReview` tab/filter, voucher preview popover, and approve/reject actions.

- [ ] **Step 5: Update order detail**

Show voucher, transfer serial number, user remark, submit time, audit time, and rejection reason.

- [ ] **Step 6: Verify unit green**

Run: `cd admin && npx vue-cli-service test:unit tests/unit/utils/offlinePay.spec.js`

Expected: PASS.

### Task 6: Build And Runtime Verification

**Files:**
- Verify: `crmeb/crmeb-common`, `crmeb/crmeb-front`, `crmeb/crmeb-admin`, `app`, `admin`

- [ ] **Step 1: Run backend test and package checks**

Run:

```bash
cd crmeb
./mvnw -pl crmeb-common -Dtest=OfflinePayUtilTest test
./mvnw -pl crmeb-front,crmeb-admin -am -DskipTests package
```

- [ ] **Step 2: Run frontend tests**

Run:

```bash
node app/tests/manual-payment.test.cjs
cd admin && npx vue-cli-service test:unit tests/unit/utils/offlinePay.spec.js
```

- [ ] **Step 3: Build customer H5**

Run: `cd app && npm run build:h5`

Expected: successful H5 build.

- [ ] **Step 4: Build admin**

Run: `cd admin && npm run build:prod`

Expected: successful admin build.

- [ ] **Step 5: Manual smoke**

Use local services to create an unpaid order, choose manual payment, upload voucher, reject it in admin, re-upload, approve it, and confirm the order becomes paid and enters待发货.
