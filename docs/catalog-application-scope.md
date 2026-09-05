# Catalog Service — application scope

Status: the Catalog backend milestone is implemented; the customer and store-owner
screens remain to be built. See [Catalog backend](catalog-backend.md) for API usage
and verification status. The end-to-end experiences below are acceptance criteria,
not a claim that the entire Catalog application milestone is finished.

## Purpose

Help a customer find and understand a product, and let an authorized store owner
manage the products shown in the shop.

## Starting point before the backend milestone

- Customers can see active products, search by product name, and add a product to
  their cart from the listing.
- Product records contain SKU, name, category, price, currency, visibility, and
  flexible specifications such as color or storage.
- The backend supports category filtering and pages of results, but the storefront
  does not expose those controls.
- Product creation, editing, and deletion are available as APIs. There is no
  product management screen, and those write APIs do not enforce an admin role.
- There is no customer product detail page, product description, or product image
  field in the current catalog model.

## Customer experience for this milestone

1. Browse published products with a picture or an intentional image placeholder,
   name, category, and clearly displayed price and currency.
2. Search by name, filter by category, sort by name or price, and navigate through
   all matching products. Show the total number of matches and a reset option.
3. Open a shareable product detail page with images, description, specifications,
   and price. Returning to the listing preserves the search and filters.
4. Add a chosen quantity to the cart and receive clear success or failure feedback.
   Adding to a cart does not promise that stock has been reserved.
5. See a helpful unavailable-product page for a hidden or missing product, and a
   retry option when the catalog cannot be reached. A failed request must not be
   presented as an empty catalog.

## Store-owner experience for this milestone

1. Sign in as an authorized administrator and open a product management screen.
2. Find products by SKU or name and distinguish published products from hidden ones.
3. Create a product with a unique SKU, name, category, description, image links,
   positive price, currency, specifications, and visibility.
4. Preview the customer-facing information before publishing a product.
5. Edit product information. Keep the SKU stable after creation so existing carts
   and orders retain their product reference.
6. Hide a product from new purchases and publish it again later. Hiding a product
   preserves its record and does not erase historical order information.
7. Receive field-specific feedback for missing information, invalid prices,
   invalid image links, or a duplicate SKU.

Image links are sufficient for this first milestone. File uploads and image
storage can be introduced separately.

## Ownership and dependencies

- Catalog owns product information and its regular price.
- Inventory owns stock quantities and reservation decisions. Publishing a catalog
  product alone does not make its stock available for purchase.
- Cart owns selected items and quantities.
- Authentication supplies the customer identity and administrator role. The
  management flow depends on enforcing that role; hiding its navigation link is
  not sufficient authorization.
- Sale schedules, countdowns, promotional prices, and per-customer sale limits
  require a later flash-sale milestone. Display promotional claims only when
  supported by actual sale data.
- Checkout, payment, delivery addresses, and order tracking belong to their own
  milestones. This catalog milestone does not change existing price units or
  historical order totals.

## Completion check

Catalog is ready for application review when these scenarios work through the UI:

- An administrator creates and publishes a product; a customer can find it.
- Search and category filters work together, sorting is predictable, and later
  pages make every matching product reachable.
- A customer opens a product link directly and sees its full information.
- A customer adds a quantity to their cart and sees the selected item there.
- An administrator edits a product and customers see the updated information.
- An administrator hides a product; it leaves the listing and cannot be purchased
  through its old detail link. Publishing it again restores visibility.
- A visitor without the administrator role cannot change product information.
- Invalid input, missing products, broken images, and service outages produce
  useful feedback on both desktop and mobile screens.

These are acceptance criteria, not a report of completed verification.
