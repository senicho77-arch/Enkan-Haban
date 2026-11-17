# Editing an item for sale (ኣቕሓ ምምሕያሽ)

A seller can change the **price, photos, title, description**, category, condition, location and
phone of an item they put up for sale. In code the item is a `Product` with
`ListingType.CLASSIFIED` (a "listing"); in the app it is called **ኣቕሓ** (item), not
መዋዓውዒ (ad). መዋዓውዒ is kept for paid/sponsored advertising only.
This note is for developers and coding assistants working on the feature.

## User flow

1. The seller opens their own item (Home → item → ዝርዝር).
2. Instead of the buyer buttons they see **"✏️ ዋጋ፡ ስእሊ ወይ ጽሑፍ ኣመሓይሽ"**.
3. That opens the same form as posting (`SellScreen`), filled with the item's current values,
   titled **"ኣቕሓ ኣመሓይሽ"**, with a **"ለውጢ ዓቅብ"** (save) button.
4. On save the app goes back to the item page, which shows the new values right away.

## Rules

- Only the seller who posted a **classified** item may edit it: `Product.isEditableBy(user)`.
  Store items (`ListingType.STORE`) are not edited here.
- The item keeps its `id`; only its fields change. Validation is the same as for a new item
  (`validate(SellForm)`), including at most `MAX_PHOTOS` (10) photos.
- Prices are stored in US cents. The form shows the price in the currency the user picked.
  If the seller leaves the price field untouched, the exact stored amount is kept
  (`resolvePrice`), so the shilling rounding never changes a price by itself.

## Where the code is

All paths are under `shared/src/commonMain/kotlin/com/example/enkan_haban/`.

| What | Where |
| --- | --- |
| Who may edit | `domain/Models.kt` → `Product.isEditableBy` |
| Saving changes | `data/ProductRepository.kt` → `ProductRepository.updateListing` (returns `null` if missing or not the seller's) |
| Form pre-fill and price logic | `ui/sell/SellViewModel.kt` → `toSellForm`, `EditTarget`, `resolvePrice`, `SellViewModel(editProductId = …)` |
| Form screen (new or edit) | `ui/sell/SellScreen.kt` → `SellScreen(editProductId = …, formKey = …)` |
| Edit button | `ui/product/ProductDetailScreen.kt` → `OwnerActions` |
| Navigation | `ui/navigation/Navigator.kt` → `Route.EditListing`; wired in `App.kt` |
| Price text for the form | `util/Money.kt` → `formatPriceInput` |
| Tests | `shared/src/commonTest/.../SharedCommonTest.kt` → `EditListingTest` |

## Not done yet

- **Backend.** The app still uses `InMemoryProductRepository`. A Supabase-backed
  `updateListing` should:
  - `update public.listings set title, description, price_minor, currency, category_id, condition,
    city, area, contact_phone where id = :id`. Row-level security already allows this only for
    the listing's `seller_id`, and only these columns can be updated.
  - Sync photos in `public.listing_images`: upload new ones to
    `listing-images/<user id>/<listing id>/<file>.jpg`, delete removed ones (both the row and
    the storage file), and rewrite `sort_order` (0 = cover photo). `(listing_id, sort_order)` is
    unique, so delete the old rows before inserting the new order.
- **Login.** `AppContainer.currentUser` is a placeholder (`id = "me"`), so today only items posted
  in this app session can be edited.
- **"ኣቕሑተይ" (my items)** in the Account screen is not clickable yet. A list of the user's
  own items there would be a second way into the edit screen.
- Deleting an item, or marking it as sold.
