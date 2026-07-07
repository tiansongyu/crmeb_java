# Unified Media URL Design

## Goal

Unify how uploaded media paths are normalized across the admin web, H5 app, and backend attachment responses so thumbnails, product images, payment screenshots, payment QR codes, rich text images, and downloaded media references render consistently.

## Scope

The canonical local media identity remains `crmebimage/...`. API responses may contain relative paths, root-relative paths, old `undefinedcrmebimage/...` strings, HTML snippets, or private development hosts. Frontends normalize these values for display. Backend attachment prefixing continues to return usable public paths and should avoid leaking private hosts into browser-rendered media.

This change does not migrate database data and does not replace cloud storage integrations. It narrows the behavior to URL normalization and tests around existing upload/display paths.

## Recommended Approach

Use a shared rule set in the existing frontend utilities instead of patching individual pages. Keep `admin/src/utils/mediaUrl.js` and `app/utils/imageUrl.js` as the platform-specific entry points, but align their behavior and tests:

- Convert `crmebimage/...`, `/crmebimage/...`, `undefinedcrmebimage/...`, and `undefined/crmebimage/...` into browser-usable media URLs.
- Strip private hosts such as `localhost`, `127.0.0.1`, `10.x.x.x`, `172.16-31.x.x`, and `192.168.x.x`.
- Preserve valid public CDN/OSS URLs.
- Preserve `data:`, `blob:`, and existing `/__image/...` proxy URLs.
- Normalize media references inside HTML, CSS `url(...)`, arrays, and nested API response objects.
- Keep upload custom-name sanitization in the admin upload component.

## Data Flow

Backend upload APIs return `FileResultVo.url` as a relative path under `crmebimage/...` or `file/...` depending on existing upload conventions. Admin and H5 request interceptors normalize the full API response before pages consume it. Upload components normalize immediate upload responses before previewing the file. Backend attachment prefixing remains responsible for preparing attachment list paths with the configured public upload prefix when available.

## Error Handling

Non-string values are returned unchanged. Blank strings remain blank. Malformed absolute URLs that still contain `crmebimage/` are normalized by extracting the media path. Unknown public absolute URLs are left untouched. Private absolute media URLs are converted to the current public media host or root-relative path, depending on the client environment.

## Tests

Add frontend unit coverage for aligned admin and H5 behavior:

- local, root-relative, and `undefined` media paths
- private host stripping
- public CDN preservation
- known proxy URL preservation
- nested response and HTML/CSS normalization
- upload filename sanitization

Run all verification through Docker, following the repository build policy.
