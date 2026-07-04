# Manual QR Payment Design

## Goal

Replace online payment usage for shopping orders with a manual collection QR-code flow. Customers scan the merchant QR code outside the system, upload a payment voucher for the order, and administrators manually approve or reject the voucher.

## Chosen Approach

Use the existing `offline` payment type and extend it with voucher review fields on `eb_store_order`.

Alternatives considered:

- New manual-payment table: better audit history, but more service and UI work than required. Existing order logs already provide an operation trail.
- Reuse refund fields: avoids schema changes, but mixes unrelated meanings and would make order/refund screens fragile.
- Order fields plus order logs: smallest reliable change, keeps the current order status machine, and lets rejected vouchers be overwritten by a new submission.

The chosen approach is order fields plus order logs.

## Data Model

Add the following columns to `eb_store_order`:

- `offline_pay_status`: `0` not submitted, `1` pending review, `2` approved, `3` rejected.
- `offline_pay_voucher`: uploaded voucher image URL.
- `offline_pay_trade_no`: optional user-entered transfer serial number.
- `offline_pay_remark`: optional user note.
- `offline_pay_refuse_reason`: admin rejection reason.
- `offline_pay_submit_time`: latest voucher submission time.
- `offline_pay_audit_time`: latest review time.
- `offline_pay_audit_admin_id`: reviewer admin id when available.

Add or reuse these system config keys:

- `offline_pay_status`: manual payment enabled flag.
- `offline_pay_qrcode`: merchant collection QR-code image URL.
- `offline_pay_name`: display name for the receiver account.
- `offline_pay_tips`: short instruction text shown to customers.

## Backend Flow

Online payment methods are disabled for shopping orders:

- Front pay config returns WeChat, balance, and Alipay as disabled.
- `pay/payment` rejects `weixin`, `yue`, and `alipay`.
- `pay/payment` accepts only `offline`, sets the order payment type to `offline`, and keeps `paid=false`.

Voucher submission:

- `POST /api/front/pay/offline/proof`
- Validates current user owns the unpaid order.
- Requires a voucher image.
- Sets `offline_pay_status=1`, clears prior rejection reason, stores optional transfer serial and note.
- Writes an order status log.

Admin review:

- `POST /api/admin/store/order/offline/audit`
- Approve requires a pending voucher, marks the order paid, sets `offline_pay_status=2`, deducts used integral, handles combination-order payment side effects, and pushes the existing `ORDER_TASK_PAY_SUCCESS_AFTER` queue.
- Reject keeps the order unpaid, sets `offline_pay_status=3`, stores the rejection reason, and allows the user to submit a new voucher.

## Frontend Flow

Customer app:

- Payment options show only "扫码转账".
- The payment page displays the configured QR code, receiver name, order number, amount, and upload controls.
- The customer uploads a voucher screenshot and can optionally enter a transfer serial number or note.
- After submission, order status becomes "付款凭证待审核".
- Rejected vouchers show the rejection reason and a re-upload action.

Admin app:

- Order list displays a manual-payment review state for offline orders.
- Pending voucher orders expose "审核付款" action.
- Order detail shows voucher image, transfer serial number, note, submit time, audit time, and rejection reason.
- Approve confirms purchase success and moves the order into the existing paid order lifecycle.
- Reject prompts for a required reason.

## Status Semantics

Manual payment status is separate from `paid`:

- `paid=false`, `offline_pay_status=0`: order uses manual payment but no voucher has been submitted.
- `paid=false`, `offline_pay_status=1`: voucher is awaiting admin review.
- `paid=true`, `offline_pay_status=2`: admin approved the voucher.
- `paid=false`, `offline_pay_status=3`: voucher was rejected and can be resubmitted.

Order cancellation remains tied to unpaid orders. A pending voucher order can still be cancelled by existing automatic cancellation if the current order timeout expires; this matches the original unpaid-order behavior unless a later business rule changes it.

## Testing

Backend tests should cover:

- Online payment types are rejected.
- Offline payment initializes an unpaid manual-payment order.
- Voucher submission validates ownership and required voucher.
- Approval marks the order paid and queues payment-success processing.
- Rejection stores the reason and keeps the order unpaid.

Frontend tests should cover:

- Payment config maps to only the manual payment option.
- Manual payment helper states render expected labels/actions.

Manual verification should cover:

- Customer creates an order, sees the QR code, uploads a voucher, and sees pending-review state.
- Admin sees voucher, rejects with reason, customer can re-upload.
- Admin approves, order becomes paid and appears as待发货.
