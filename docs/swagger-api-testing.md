# API testing with Swagger

After reloading the Gradle projects in IntelliJ and restarting the services, open
[Swagger at the API Gateway](http://localhost:8080/swagger-ui/index.html).

Use the **Select a definition** dropdown to switch services. Catalog is selected
initially. Each selected service must be running to load its definition and execute
requests. The gateway and the selected service are sufficient for documentation;
executing business requests also needs that service's database and other dependencies.

## Swagger URLs

| Service | Direct Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| API Gateway / all services | [8080](http://localhost:8080/swagger-ui/index.html) | [Gateway definition](http://localhost:8080/v3/api-docs) |
| Order | [8081](http://localhost:8081/swagger-ui/index.html) | [Order definition](http://localhost:8081/v3/api-docs) |
| Inventory | [8082](http://localhost:8082/swagger-ui/index.html) | [Inventory definition](http://localhost:8082/v3/api-docs) |
| Payment | [8083](http://localhost:8083/swagger-ui/index.html) | [Payment definition](http://localhost:8083/v3/api-docs) |
| Cart | [8084](http://localhost:8084/swagger-ui/index.html) | [Cart definition](http://localhost:8084/v3/api-docs) |
| Catalog | [8085](http://localhost:8085/swagger-ui/index.html) | [Catalog definition](http://localhost:8085/v3/api-docs) |
| Auth | [8086](http://localhost:8086/swagger-ui/index.html) | [Auth definition](http://localhost:8086/v3/api-docs) |

`/swagger-ui.html` redirects to the UI. Each service also exposes YAML at
`/v3/api-docs.yaml`. The gateway's own JSON contains its health endpoint; use
`/openapi/auth`, `/openapi/catalog`, `/openapi/cart`, `/openapi/order`,
`/openapi/inventory`, or `/openapi/payment` for a downstream definition through
the gateway.

## Log in and test Catalog

1. Select **Auth Service** and expand `POST /auth/login`.
2. Click **Try it out**, enter your email and password, then **Execute**. New users
   can first use `POST /auth/register`.
3. Copy the returned `accessToken`. Click **Authorize** and paste only that token,
   without the `Bearer` prefix. `GET /auth/me` shows your account and roles.
4. Select **Catalog Service**. Authorize that definition with the same token if
   needed. Public product browsing works without a token.
5. Use **Catalog administration** to create a product with `active: false`, preview
   it, and publish it using `PUT /products/admin/items/{sku}/visibility` with
   `{"active":true}`.
6. Use `GET /products` and `GET /products/{sku}` to confirm it is visible. Hide it
   with `{"active":false}` and check it no longer appears publicly.

Catalog administration requires the `ADMIN` role. Registration creates `USER`
accounts. See [local administrator setup](catalog-backend.md#getting-an-administrator-token-locally)
to grant the intended store owner that role, then log in again for a fresh token.
A 401 means the token is missing or invalid; a 403 means the account lacks access.
Authorization is not persisted across browser reloads.

## Cart, Orders, Inventory, and Payment

- **Cart:** use one consistent `userId` while adding, updating, and removing items.
  Current cart endpoints use this path parameter rather than JWT authentication.
- **Orders:** fill in `X-User-Id` using the same user ID. Supply a fresh
  `Idempotency-Key` for each new order. Reusing a key retries the original request;
  it does not create a new order. The example key must be changed for your next
  intended order.
- **Inventory:** `GET /inventory/{sku}` shows available and reserved quantities.
  The initial database migration seeds `IPHONE-16-128-BLK`; its current stock
  depends on your previous tests.
- **Payment:** there is no HTTP payment-creation API. This service receives
  `flashsale.payment.commands` and sends `flashsale.payment.events` through Kafka.
  Its Swagger definition contains a health check and explains this behavior.
  To test payment processing, create an order while the Order, Inventory, Payment,
  Kafka, and required database services are running, then fetch the order's status.

**Execute sends a real API request.** Use your intended test accounts and products.
Swagger describes the current endpoints; it does not change their business rules.

## How the gateway page works

The gateway fetches each definition at `/openapi/{service}` and adjusts its server
URL to `/services/{service}`. Requests from **Try it out** go through that prefix
to the selected backend with the method, body, query string, and authorization
headers preserved. This also makes a Payment health request reach Payment rather
than accidentally checking the gateway. Direct service Swagger pages use their
own origin. No additional browser CORS configuration is needed for this flow.

The original application routes such as `/products/**`, `/auth/**`, and `/orders/**`
remain available. The documentation proxies use `AUTH_SERVICE_URL`,
`CATALOG_SERVICE_URL`, `CART_SERVICE_URL`, `ORDER_SERVICE_URL`,
`INVENTORY_SERVICE_URL`, and `PAYMENT_SERVICE_URL` when supplied, otherwise the
local ports in the table above. These overrides apply to documentation/testing
proxies; the existing application routing configuration remains separate.

Payment now includes the HTTP web starter to serve Swagger and Actuator on its
already-configured port 8083. Its Kafka payment processing stays unchanged.

`API_DOCS_ENABLED=false` disables Swagger and OpenAPI on a service. On the gateway
it also disables the added `/openapi/**` and `/services/**` documentation routes.

## Verification and troubleshooting

Each service has an `OpenApiDocumentationTests` suite that starts an isolated web
context, serves the UI and JSON/YAML definitions, and verifies its documented
endpoints. Database access and business collaborators are excluded or mocked.
These checks do not require Docker or call your running business services.

From any service directory:

```bash
./gradlew test --tests '*OpenApiDocumentationTests' bootJar
```

The gateway additionally tests service selection, path rewriting, authorization
and request forwarding, and upstream errors against a temporary local HTTP server:

```bash
./gradlew test --tests '*OpenApiProxyTests'
```

- **Swagger returns 404:** reload Gradle dependencies and restart that service.
  Confirm `API_DOCS_ENABLED` is not false.
- **A definition fails to load in the dropdown:** confirm the selected backend is
  running; check its direct `/v3/api-docs` URL from the table.
- **Requests fail with 503:** the service or one of its dependencies may be down.
  Inspect the response and the selected service's logs.
- **Catalog write returns 403:** use an ADMIN account and log in again after a role
  change. Opening Swagger itself does not require login.

Dependencies follow the [springdoc Spring Boot compatibility matrix](https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot):
Auth remains on Boot 3.3 with springdoc 2.6.0; the Boot 3.5 services use springdoc
2.8.17. The gateway uses the WebFlux starter, and HTTP backend services use the
WebMVC starter. Definitions use OpenAPI 3.0 consistently across both versions.
