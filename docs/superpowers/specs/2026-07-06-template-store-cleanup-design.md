# Template Store Cleanup Design

## Goal

Turn this deployment into a reusable store template instead of a demo store populated with real-looking products, people, orders, articles, company names, and external brand references.

## Scope

The template keeps operational platform data that is required for the application to run:

- Admin menus, roles, permissions, system groups, form definitions, city data, express data, scheduled job definitions, and Quartz tables.
- One usable admin account so the backend remains manageable after cleanup.
- Manual QR payment mode support, with the QR image left blank for the operator to configure.

The template removes or neutralizes business/demo content:

- Products, SKUs, product mappings, product descriptions, product logs, comments, carts, orders, order logs, users, user addresses, user bills, balances, recharge records, brokerage records, visits, and sign records.
- Group-buying, bargain, seckill, pink-team, coupons, coupon claims, shipping templates, stores, store staff, articles, WeChat replies, material attachments, and previous DIY/demo pages.
- Public-facing company names, demo store names, external legacy/customer-service links, demo screenshots, real-looking phone/address values, and seeded placeholder credentials such as `111111`.

## Template Placeholder Data

The storefront should not look broken or empty. It should show neutral placeholder structure:

- Site name: `商城模板`
- SEO/share title: `商城模板`
- Copyright/company text: `Template Store`
- Three product categories:
  - `模板分类一`
  - `模板分类二`
  - `模板分类三`
- A default DIY homepage named `模板首页` containing neutral components only: search, category navigation, title/rich-text guidance, and an empty product list section.
- No real products are inserted. Admins are expected to add products from the backend.

## Database Strategy

Use an additive cleanup SQL migration instead of rewriting the full upstream base SQL dump:

- Create `crmeb/sql/template_store_cleanup_20260706.sql`.
- Import it after `Crmeb_v1.4.sql`, manual QR payment SQL, and activity SKU repair SQL in Docker Compose.
- Make the SQL idempotent: it can run on a fresh database or an existing deployment.
- Disable foreign-key checks during cleanup, clear demo/business tables, insert template placeholders, and update `eb_system_config` values.

This keeps the base schema history readable while ensuring Docker-based deployments become template deployments by default.

## Code and Static Text Strategy

Replace runtime-visible project branding with template wording where it affects the deployed product:

- Admin/H5 HTML metadata and display titles.
- Spring Boot banner and default CAPTCHA watermark.
- `demoSite` flags should be `false` so this is no longer treated as a demo site.
- Disable startup calls to the legacy remote upgrade endpoint by default.

Technical identifiers that are not user-facing stay unchanged unless they directly leak into UI:

- Java package names, Maven artifact IDs, container names, upload path names such as `crmebimage`, and test user-agent strings can remain because changing them would create avoidable risk.
- License files and third-party attributions are not removed.

## Verification

All compile/build/test verification must run through Docker per `AGENTS.md`.

Required checks:

- SQL syntax/import check with a disposable MySQL container using the full ordered init set.
- Row-count assertions verifying demo tables are empty and template categories/default page exist.
- Docker build verification with `scripts/docker/verify-builds.sh`.
- A lightweight API smoke against a disposable compose stack or existing server after applying the cleanup SQL.
