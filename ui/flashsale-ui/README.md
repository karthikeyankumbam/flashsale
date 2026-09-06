# FlashSale UI

Angular 21 storefront for the FlashSale microservices project. It talks to the API
Gateway at `http://localhost:8080` and includes both customer catalog experiences
and authenticated catalog administration.

## Catalog experiences

- `/products` — published product search, category filtering, sorting, pagination,
  image fallbacks, quantity selection, and cart actions
- `/products/{sku}` — shareable product details, image gallery, description,
  specifications, and a return link that preserves listing filters
- `/catalog-admin` — ADMIN sign-in, published/hidden product search, create/edit
  form, live customer preview, field validation, and publish/hide actions

The owner form creates products as hidden drafts by default. Registration through
Auth Service grants `USER`; see the repository's
[Catalog backend guide](../../docs/catalog-backend.md#getting-an-administrator-token-locally)
for local ADMIN role setup.

## Run locally

Start the API Gateway and its backend services first, then run:

```bash
npm install
npm start
```

Open [http://localhost:4200/products](http://localhost:4200/products).

## Build and test

```bash
npm run build
npm test -- --watch=false
```

Build output is written to `dist/flashsale-ui`.
