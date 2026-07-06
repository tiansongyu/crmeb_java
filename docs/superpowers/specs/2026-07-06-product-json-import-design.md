# Product Excel Import Design

## Goal

Add a fast backend-driven way for operators to create many products in a template store by uploading one structured file from the admin product list page.

## Current State

The project already has:

- A complete single-product save flow at `POST /api/admin/store/product/save`.
- External product copy/import endpoints that depend on third-party URLs.
- Excel export support and a frontend Excel reader component.

It does not have a local file-based batch product import flow. Direct SQL import is rejected because it bypasses product validation and the normal writes to product, SKU, description, coupon mapping, and category mapping tables.

## Chosen Approach

Use an Excel file import flow as the admin-facing default:

- Admin downloads a `.xlsx` template from the product list page.
- Admin uploads a filled `.xlsx` or `.xls` file.
- Backend parses and validates the file.
- Rows with the same `商品编码` are merged into one product.
- Each data row becomes one SKU under that product.
- Backend reuses `StoreProductService.save(StoreProductAddRequest)` for each imported product.
- `dryRun=true` validates without writing.
- `dryRun=false` imports valid products and returns per-row results.

JSON import remains available as a compatibility and advanced-use endpoint, but the backend UI uses Excel because it is easier for operators to edit and review. The column convention keeps the data predictable: product-level fields repeat on every row, and `商品编码` defines the merge boundary.

## File Format

The Excel template uses these columns:

```text
商品编码, 商品名称, 分类, 分类ID, 关键字, 单位, 主图, 轮播图, 详情,
规格1名, 规格1值, 规格2名, 规格2值, 规格3名, 规格3值,
售价, 原价, 成本价, 库存, 重量, 体积, SKU图, 商品条码,
运费模板ID, 排序, 虚拟销量, 热卖, 优惠, 精品, 新品, 优品推荐,
赠送积分, 一级返佣, 二级返佣
```

Rules:

- Maximum 200 products per upload.
- `商品编码` is required. The same code can appear on multiple rows to create multiple SKUs for one product.
- Required product fields: `商品名称`, `分类` or `分类ID`, `主图`, `轮播图`.
- Required SKU fields: `售价`, `原价`, `成本价`, `库存`.
- Supports up to three spec dimensions through `规格1/2/3名` and `规格1/2/3值`.
- If no spec columns are filled, the backend creates the default `规格=默认` SKU.
- Multiple slider images are separated with English comma, Chinese comma, semicolon, or line break.
- Boolean columns accept `是`, `1`, `true`, `yes`, or `y`.
- Every product must have `storeName`, `keyword`, `unitName`, `image`, at least one slider image, and at least one SKU.
- `categoryId` can be supplied. Otherwise `categoryName` is matched by name.
- If `categoryName` does not exist, the importer creates a root product category.
- If `tempId` is absent and no shipping template exists, the importer creates a default free-shipping template.
- Imported products are created as normal products and remain unpublished by default, matching the existing `StoreProductService.save` behavior.

## Backend API

Add:

`POST /api/admin/store/product/import/excel?dryRun=true|false`

Keep:

`POST /api/admin/store/product/import/json?dryRun=true|false`

Request:

- multipart field `file`
- `dryRun` defaults to `true`

Response:

- `total`: number of products read
- `success`: number of rows validated/imported successfully
- `failed`: number of failed rows
- `dryRun`: whether the request wrote data
- `items`: per-product result with row number, product name, status, product id if imported, and message

Authorization reuses the existing product-create permission:

- `admin:product:save`

The import flow creates products through the same backend save path as the single-product form, so using the existing create permission keeps old and fresh databases compatible.

## Admin UI

Add a `批量导入` button to `admin/src/views/store/index.vue`.

The dialog includes:

- Download template Excel.
- Upload Excel file.
- Validate button that calls `dryRun=true`.
- Import button enabled after validation with no failures.
- Result table showing each row status and message.

## Template Seed Compatibility

The template-store cleanup keeps neutral categories and a default free-shipping template, so the importer can work immediately after a fresh template deployment.

## Testing

All automated verification runs inside Docker.

Backend unit tests cover:

- Excel row grouping by `商品编码`.
- Excel rows converting into multi-SKU `StoreProductAddRequest`.
- Excel validation failures returned without saving.
- JSON conversion into `StoreProductAddRequest`.
- category auto-create by name.
- default shipping template selection/creation.
- dry-run does not call product save.
- per-row failure does not block other rows.

Frontend build verification is covered by the Docker build script.
