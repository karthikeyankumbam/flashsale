# Catalog backend

Catalog owns product information, its regular price, and whether it is published.
The endpoints below work directly at `http://localhost:8085` or through the existing
gateway at `http://localhost:8080`. All routes remain under `/products`.

## Customer endpoints

These endpoints work without logging in and expose published products only.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/products` | Search, filter, sort, and page through products |
| GET | `/products/{sku}` | Product details; hidden/missing products return 404 |
| GET | `/products/browse/categories` | Sorted categories with published products |

List parameters:

| Parameter | Default | Meaning |
| --- | --- | --- |
| `query` | omitted | Literal, case-insensitive search in name or SKU; up to 200 characters |
| `category` | omitted | Exact, case-insensitive category; up to 100 characters |
| `currency` | omitted | Three-letter currency, such as `INR` |
| `minPrice`, `maxPrice` | omitted | Inclusive, nonnegative price bounds |
| `page` | `0` | Zero-based page number |
| `size` | `20` | Page size, from 1 to 100 |
| `sort` | `name-asc` | `name-asc`, `name-desc`, `price-asc`, `price-desc`, `newest`, or `updated` |

```bash
curl --get http://localhost:8080/products \
  --data-urlencode 'query=phone' \
  --data-urlencode 'category=Phones' \
  --data-urlencode 'currency=INR' \
  --data-urlencode 'sort=price-asc' \
  --data-urlencode 'page=0' \
  --data-urlencode 'size=12'
```

Responses include `content`, `number`, `size`, `totalElements`, `totalPages`,
`first`, `last`, `empty`, and `numberOfElements`. Existing UI reads of `content`
continue to work. Equal sort values use SKU as a tie-breaker. When prices in
different currencies exist, use the currency filter for meaningful price comparison;
Catalog does not perform currency conversion.

## Administrator endpoints

Send `Authorization: Bearer <access-token>` with a token issued by Auth Service
containing the `ADMIN` role. Customers with only `USER` receive 403; a missing,
invalid, or expired token receives 401.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/products/admin/items` | List products, including hidden ones |
| GET | `/products/admin/items/{sku}` | Preview any product |
| POST | `/products/admin/items` | Create a product; returns 201 |
| PUT | `/products/admin/items/{sku}` | Update product information |
| PUT | `/products/admin/items/{sku}/visibility` | Publish or hide using `{"active":true}` or `{"active":false}` |
| DELETE | `/products/admin/items/{sku}` | Hide a product; returns 204 and retains its record |

The admin list accepts the customer filters plus `visibility=all|published|hidden`.
Its defaults are `visibility=all` and `sort=updated`.

The original write routes, `POST /products` and `PUT`/`DELETE /products/{sku}`,
remain available with the same administrator requirement. DELETE now hides the
product instead of removing it permanently.

### Product fields

- `sku`: required on creation; 1–128 letters, numbers, dots, underscores, or
  hyphens, beginning with a letter or number. It is case-sensitive and immutable.
- `name`, `category`: required, up to 200 and 100 characters respectively.
- `price`: required positive integer. Existing whole-currency-unit values are
  preserved: `19999` continues to mean INR 19,999 in the current storefront.
  Fractional values are rejected rather than silently rounded. A future move to
  minor units must coordinate Catalog, Orders, Payment, and the UI.
- `currency`: required supported three-letter code; stored in uppercase.
- `active`: defaults to `true` on creation for compatibility. Use `false` to
  create a hidden product before publishing it.
- `description`: optional, up to 5,000 characters.
- `images`: optional, ordered list of up to eight absolute HTTP/HTTPS URLs.
  The first can be used as the cover image. URLs must not contain credentials.
  Catalog stores links; it does not fetch, upload, or verify image availability.
- `attributes`: optional specification map with up to 30 entries. Keys must be
  nonblank, at most 80 characters, and contain neither `.` nor `$`.

PUT requires name, category, price, and currency. Omitted or null optional fields
retain their stored values. Send `description: ""`, `images: []`, or
`attributes: {}` to clear them. Old MongoDB documents remain readable and expose
an empty description and image list until enriched.

### Create, preview, publish, and hide

Set `CATALOG_ADMIN_TOKEN` in your terminal to your administrator access token.
The following calls modify the example product when you choose to run them.

```bash
curl -X POST http://localhost:8080/products/admin/items \
  -H "Authorization: Bearer $CATALOG_ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "PHONE-DEMO-128",
    "name": "Demo Phone 128 GB",
    "category": "Phones",
    "price": 19999,
    "currency": "INR",
    "active": false,
    "description": "A compact phone with 128 GB storage.",
    "images": [],
    "attributes": {"color": "Black", "storage": "128 GB"}
  }'

curl http://localhost:8080/products/admin/items/PHONE-DEMO-128 \
  -H "Authorization: Bearer $CATALOG_ADMIN_TOKEN"

curl -X PUT http://localhost:8080/products/admin/items/PHONE-DEMO-128/visibility \
  -H "Authorization: Bearer $CATALOG_ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"active":true}'

curl http://localhost:8080/products/PHONE-DEMO-128

curl -X PUT http://localhost:8080/products/admin/items/PHONE-DEMO-128/visibility \
  -H "Authorization: Bearer $CATALOG_ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"active":false}'
```

Publishing restores catalog visibility only. Inventory stock must be supplied
separately. Existing Cart and Order services do not yet enforce catalog visibility
when processing direct writes; that cross-service purchase check belongs to their
milestones. Catalog does not reserve stock or schedule flash sales.

## Getting an administrator token locally

Auth registration currently grants `USER`, not `ADMIN`. Register the intended
store-owner account through `/auth/register` first. An operator can then grant
that specific existing account the seeded ADMIN role in the local Auth database:

```bash
docker exec -i flashsale-auth-postgres psql -U auth -d auth \
  -v admin_email='owner@example.com' <<'SQL'
insert into user_roles(user_id, role_id)
select u.id, r.id
from users u cross join roles r
where lower(u.email) = lower(:'admin_email') and r.name = 'ADMIN'
on conflict do nothing;

select u.email, r.name
from users u
join user_roles ur on ur.user_id = u.id
join roles r on r.id = ur.role_id
where lower(u.email) = lower(:'admin_email');
SQL
```

Replace the example email deliberately. If the account does not exist, this grants
nothing. Sign in again with `/auth/login` after the grant to obtain a new token;
already-issued tokens retain their original roles until they expire.
No accounts or role grants are automatically created by Catalog.

Catalog validates Auth's HS256 signature, issuer, expiration, subject, and roles
using [Spring Security's JWT resource server support](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html).
`SECURITY_JWT_SECRET` and `SECURITY_JWT_ISSUER` can override the configuration and
must match Auth Service. These are the same plain UTF-8 signing-key values that
Auth uses, not Base64-encoded replacements. `CATALOG_MONGODB_URI` overrides the
database connection. CORS remains handled by the gateway.

## Errors and verification

Errors use `application/problem+json`: 400 for invalid input, 401 for invalid or
missing authentication, 403 for insufficient role, 404 for unavailable products,
409 for duplicate SKUs, and 503 for a database outage. Validation responses include
an `errors` map keyed by field or parameter. Concurrent duplicate creation cannot
overwrite an existing product.

From the Catalog Service directory:

```bash
./gradlew test bootJar
./gradlew integrationTest
./gradlew bootRun
```

`test` covers the HTTP contract, actual JWT verification, validation, and service
behavior without Docker. `integrationTest` requires Docker and runs real HTTP
requests against a temporary MongoDB 7 container, including publishing, filtering,
pagination, legacy documents, and simultaneous duplicate creation. It uses its own
database and never targets the local application catalog. Integration tests fail
if Docker is unavailable; they are excluded from the default test task.

The Angular catalog UI is available at `/products`, with shareable product details
at `/products/{sku}`. The administrator workspace at `/catalog-admin` uses these
secured endpoints for draft creation, preview, editing, publishing, and hiding.

Verification recorded on 2026-09-05: `./gradlew test bootJar --no-daemon` passed
with 48 tests, zero failures, and zero skipped tests in the default suite. The six
separate MongoDB integration tests compile but have not been run in this session;
the Docker readiness check was declined. Database integration verification remains
pending before considering the backend fully verified.
