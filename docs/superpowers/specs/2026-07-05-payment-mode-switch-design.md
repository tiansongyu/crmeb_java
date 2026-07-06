# Payment Mode Switch Design

## Goal
Add an admin-side one-click payment mode switch that keeps manual QR transfer and WeChat online payment mutually exclusive while preserving existing WeChat backend interfaces.

## Modes
- `offline_qr`: manual QR transfer is enabled, WeChat online payment is disabled.
- `wechat_online`: WeChat online payment is enabled, manual QR transfer is disabled.

## Data Compatibility
The new primary config key is `pay_mode`. Existing keys stay in use for backward compatibility:
- `offline_pay_status`
- `pay_weixin_open`

Switching modes writes all three keys in one transaction. If `pay_mode` is missing, the backend infers the mode from legacy config, preferring `offline_qr` when manual QR is enabled.

## Admin Flow
The admin system settings page shows a payment mode panel when the payment config tab is selected. The panel displays the active mode and exposes two one-click buttons. Switching to manual QR requires a configured collection QR code. Switching to WeChat online payment requires at least one complete WeChat payment credential set.

## Runtime Behavior
Frontend payment config endpoints return only the active mutually exclusive mode. Order payment accepts `offline` only in `offline_qr` mode and accepts `weixin` or balance payment in `wechat_online` mode.

## Tests
Unit tests cover mode normalization, validation, and legacy config inference. Smoke checks cover backend compilation/tests, admin build, and app build where available.
