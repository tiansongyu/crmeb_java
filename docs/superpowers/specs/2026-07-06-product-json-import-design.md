# Product JSON Import Design

## Goal

Add a fast backend-driven way for operators to create many products in a template store by uploading one structured file from the admin product list page.

## Current State

The project already has:

- A complete single-product save flow at `POST /api/admin/store/product/save`.
- External product copy/import endpoints that depend on third-party URLs.
- Excel export support and a frontend Excel reader component.

It does not have a local file-based batch product import flow. Direct SQL import is rejected because it bypasses product validation and the normal writes to product, SKU, description, coupon mapping, and category mapping tables.

## Chosen Approach

Use a JSON file import flow:

- Admin uploads a `.json` file from the product list page.
- Backend parses and validates the file.
- Backend reuses `StoreProductService.save(StoreProductAddRequest)` for each imported product.
- `dryRun=true` validates without writing.
- `dryRun=false` imports valid products and returns per-row results.

JSON is preferred over Excel because products contain nested data: categories, slider images, rich text, attributes, and SKU rows. JSON keeps that structure explicit and avoids fragile column conventions for multi-spec products.

## File Format

The file root is:

```json
{
  "products": [
    {
      "storeName": "模板商品A",
      "categoryName": "模板分类一",
      "keyword": "模板商品",
      "unitName": "件",
      "image": "crmebimage/public/product/demo-a.jpg",
      "sliderImages": ["crmebimage/public/product/demo-a.jpg"],
      "content": "<p>商品详情</p>",
      "skus": [
        {
          "specs": { "规格": "默认" },
          "price": 99,
          "otPrice": 129,
          "cost": 50,
          "stock": 100,
          "weight": 0,
          "volume": 0,
          "image": "crmebimage/public/product/demo-a.jpg"
        }
      ]
    }
  ]
}
```

Rules:

- Maximum 200 products per upload.
- Every product must have `storeName`, `keyword`, `unitName`, `image`, at least one slider image, and at least one SKU.
- `categoryId` can be supplied. Otherwise `categoryName` is matched by name.
- If `categoryName` does not exist, the importer creates a root product category.
- If `tempId` is absent and no shipping template exists, the importer creates a default free-shipping template.
- Imported products are created as normal products and remain unpublished by default, matching the existing `StoreProductService.save` behavior.

## Backend API

Add:

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

- Download template JSON.
- Upload JSON file.
- Validate button that calls `dryRun=true`.
- Import button enabled after validation with no failures.
- Result table showing each row status and message.

## Template Seed Compatibility

The template-store cleanup keeps neutral categories and a default free-shipping template, so the importer can work immediately after a fresh template deployment.

## Testing

All automated verification runs inside Docker.

Backend unit tests cover:

- JSON conversion into `StoreProductAddRequest`.
- category auto-create by name.
- default shipping template selection/creation.
- dry-run does not call product save.
- per-row failure does not block other rows.

Frontend build verification is covered by the Docker build script.
