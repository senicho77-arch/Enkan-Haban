# Admin-controlled listing order per category

> **ብትግርኛ (ንዋና):** እዚ ፋይል ንኮድ ዝጽሕፍ AI (Claude Code ወዘተ) ዝተዳለወ መምርሒ እዩ።
> ዕላማ፦ ኣድሚን (ንስኺ) ኣብ ነፍሲ ወከፍ ምድብ (category) ፖስት ዓማዊል ብዝደለኽዮ መስርዕ ክትሰርዕዮ —
> ንላዕሊ/ንታሕቲ ምንቕስቓስ፡ ኣብ ላዕሊ ምልጋብ (pin)፡ ክሳብ ዕለት ዝተወሰነ ምልጋብ፡ ካብ መስርዕ ምእላይ።
> ዘይተሰርዑ ፖስትታት ድሕሪ ዝተሰርዑ ብሓድሽ-ቅድሚት ይመጹ። ተራ ተጠቀምቲ እዚ ክቕይሩ ኣይክእሉን።
> ፋይሉ ኣብ AI ኣእትዮ ከምዚ በልዮ፦ "Implement docs/ADMIN_LISTING_ORDER.md"።

---

## 1. Context (read first)

Repo: `senicho77-arch/Enkan-Haban` — Kotlin/Compose Multiplatform app (Android + iOS, code in
`shared/src/commonMain`), backend is Supabase (`supabase/migrations`, tests in `supabase/tests`).

Relevant existing pieces:
- `public.categories` (`id` text, `parent_id`, `sort_order`) — two levels: top category → sub-category.
- `public.listings` — customer posts. Sellers can update their own rows, but only the columns
  granted in `20261027000000_init_marketplace.sql` (`grant update (...) on public.listings`).
- `public.search_listings(q, p_category_id, p_country, p_city, p_type, p_limit, p_offset)` — the feed.
  Latest version is in `20261028000000_hardening.sql`. A top-category filter also returns its
  sub-categories' listings. Ordering today: text rank (if `q`), then `created_at desc`.
- "Admin" today = service role / SQL editor only. There is **no in-app admin yet** and app login is
  not built yet (`AccountScreen` has a disabled login button).
- App side: `ProductRepository` / `InMemoryProductRepository` (`data/ProductRepository.kt`),
  `filterProducts(...)`, `HomeViewModel`, `Navigator` (`ui/navigation/Navigator.kt`), `AccountScreen`.
- UI text is **Tigrinya**. Keep existing style and conventions; don't refactor unrelated code.

## 2. What the owner wants

The owner (the admin) decides the order of customer posts **inside each category**, by hand.

Rules:
1. Every category has its own order, independent of other categories. A top category (e.g. `phones`)
   and its sub-category (e.g. `phones-smartphones`) each have their own order — the order used is the
   one for the category the buyer is currently viewing.
2. The home feed with no category selected is also orderable — use the reserved key `'all'`.
3. Admin-placed listings come **first**, in the admin's order (position 1, 2, 3 …).
4. All other listings follow, **newest first** (current behaviour).
5. A placement can have an optional end date (`pinned_until`). After that time it is ignored
   automatically (the listing falls back into the "newest first" part). No cron job needed.
6. Only placements for listings that are still `active` and still in that category (or its
   sub-category) count. If a seller deletes/sells/moves the listing, its placement is just ignored.
7. When the buyer types a search text (`q` not empty), relevance order wins — admin order is
   applied only as a tiebreaker. (Browsing = admin order; searching = best match.)
8. Only admins can create/change/delete placements. Sellers and buyers cannot, and cannot see who
   is admin.

## 3. Database (new migration)

Create `supabase/migrations/20261031000000_admin_listing_order.sql`. Do not edit old migrations.

### 3.1 Admins
```sql
create table public.admins (
  user_id    uuid primary key references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now()
);
alter table public.admins enable row level security;
-- no policies for anon/authenticated → nobody can read or write it from the app

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.admins a where a.user_id = auth.uid())
$$;
grant execute on function public.is_admin() to authenticated;
```
The first admin is added by the owner in the Supabase SQL editor (document this in `supabase/README.md`):
```sql
insert into public.admins (user_id) values ('<owner auth user id>');
```

### 3.2 Placements
```sql
create table public.category_placements (
  category_id  text not null,           -- a categories.id, or the reserved value 'all'
  listing_id   uuid not null references public.listings (id) on delete cascade,
  position     integer not null check (position >= 1),
  pinned_until timestamptz,             -- null = no end date
  created_by   uuid references public.profiles (id) on delete set null default auth.uid(),
  updated_at   timestamptz not null default now(),
  primary key (category_id, listing_id),
  constraint category_placements_category_ok
    check (category_id = 'all' or category_id ~ '^[a-z0-9-]+$')
);
create index category_placements_order_idx on public.category_placements (category_id, position);
```
- Validate in the RPCs (not by FK, because of `'all'`) that `category_id` is `'all'` or exists in
  `public.categories`.
- RLS on, **select** allowed to everyone (positions are not secret, and the feed joins them);
  **no insert/update/delete** for anon/authenticated — writes only through the admin RPCs below.

### 3.3 Admin RPCs (`security definer`, `set search_path = ''`, first line: raise if `not public.is_admin()`)
| Function | What it does |
|---|---|
| `admin_set_category_order(p_category_id text, p_listing_ids uuid[])` | Replaces the whole order of that category in one transaction: delete existing placements for the category, insert the given ids with position = array index (1-based). Keeps `pinned_until` of ids that were already placed. Rejects duplicates, unknown/inactive listings, and listings not in that category (or its sub-categories; any category for `'all'`). Empty array = clear the order. |
| `admin_pin_listing(p_category_id text, p_listing_id uuid, p_position int default 1, p_pinned_until timestamptz default null)` | Inserts/moves one listing to `p_position`, shifting the others down; positions stay 1..n without gaps. |
| `admin_unpin_listing(p_category_id text, p_listing_id uuid)` | Removes it and closes the gap. |
| `admin_get_category_order(p_category_id text)` | Returns placed listings (id, title, position, pinned_until, is_expired) in order — for the admin screen. |

`revoke execute ... from public, anon;` then `grant execute ... to authenticated;` (the admin check is inside).

### 3.4 Feed ordering
`create or replace function public.search_listings(...)` with the **same signature and filters**,
only the ordering changes:
```sql
left join public.category_placements p
  on p.listing_id = l.id
 and p.category_id = coalesce(p_category_id, 'all')
 and (p.pinned_until is null or p.pinned_until > now())
...
order by
  case when coalesce(trim(q), '') = '' then 0
       else ts_rank(l.search, websearch_to_tsquery('simple', q)) end desc,
  (p.position is null),      -- placed first
  p.position asc,
  l.created_at desc,
  l.id                       -- stable paging
```
Keep the paging clamp and the `%`/`_` escaping from the hardening version exactly as they are.

### 3.5 SQL tests
Add to `supabase/tests/security_test.sql` (and stub what's needed in `supabase_stub.sql`), in the
existing PASS/FAIL style; all must pass with `supabase/tests/run.sh`:
- normal user cannot read `admins`, cannot insert/update/delete `category_placements`, and calling
  any `admin_*` RPC raises;
- admin can set an order; `search_listings(p_category_id => 'phones')` returns placed listings first
  in that order, then the rest newest first;
- `'all'` order applies when `p_category_id` is null;
- expired `pinned_until` is ignored; a sold/removed listing's placement is ignored;
- order of `phones` does not affect `phones-smartphones` and vice versa;
- with a search text, the best match still comes first.

## 4. App (Kotlin, `shared/src/commonMain`)

Backend is not wired to the app yet, so implement against the in-memory repository now, behind
interfaces that a Supabase implementation can fill in later.

1. **Domain** (`domain/Models.kt`): `data class Placement(val categoryKey: String, val productId: String, val position: Int, val pinnedUntilEpochMs: Long? = null)`.
   Constant `const val ALL_CATEGORIES = "all"`.
2. **Ordering logic** (pure function, easy to unit test), e.g. in `data/ProductRepository.kt`:
   `fun orderForCategory(products: List<Product>, placements: List<Placement>, categoryKey: String, nowMs: Long): List<Product>`
   — same rules as section 2. Use it in `filterProducts`/`HomeViewModel` when the query is blank;
   when the query is not blank keep the current behaviour.
   Note: sample products have no `createdAt`; add `val createdAtEpochMs: Long = 0` to `Product`
   (sample data keeps list order as "newest first") rather than inventing other fields.
3. **Repository**: add an `AdminRepository` interface + `InMemoryAdminRepository`:
   `observePlacements(): Flow<List<Placement>>`, `setOrder(categoryKey, productIds)`,
   `pin(categoryKey, productId, position, pinnedUntil)`, `unpin(categoryKey, productId)`,
   `isAdmin: StateFlow<Boolean>`. Register it in `AppContainer`. Until login exists, `isAdmin`
   is `true` in debug/demo builds only (one clearly named flag in `AppContainer`, with a TODO to
   replace it with `rpc("is_admin")` after Google sign-in).
4. **Admin screen** `ui/admin/AdminOrderScreen.kt` + `AdminOrderViewModel.kt`, route
   `Route.AdminOrder` in `Navigator`. Reached from a new row in `AccountScreen`
   ("🛠️  ምሕደራ መስርዕ" — shown **only** when `isAdmin` is true).
   - Category picker at the top: "ኩሉ (ገጽ መበገሲ)" for `'all'`, then each category and its sub-categories.
   - Section "ዝተሰርዑ" (placed): numbered list; each row shows photo/emoji, title, price, seller,
     and buttons ⬆️ ⬇️ (move), 📌 end date (optional date picker, "ክሳብ …"), ✖ (remove from order).
     Long-press drag to reorder is a nice extra, arrows are required (work well on phones).
   - Section "ካልኦት ፖስትታት" (not placed, newest first) with a "➕ ናብ መስርዕ" button per row that
     adds it to the end of the placed list.
   - "ዕቀብ" (Save) button sends the full order in one call (`setOrder`); "ሰርዝ" discards changes.
     Show a confirmation snackbar "መስርዕ ተዓቂቡ" on success, error text on failure.
   - Expired placements are shown greyed with "ግዜኡ ኣኺሉ".
5. Buyers see nothing new except the order. Optionally show a small "📌" tag on placed items in the
   list (owner may want "featured" look) — implement behind a `showPinnedBadge` flag, default off.

## 5. Tests (app)
In `shared/src/commonTest/.../SharedCommonTest.kt`:
- placed first in admin order, rest newest first;
- each category key has its own order; `'all'` used when no category;
- expired placement ignored; placement of a product no longer in the list ignored;
- moving up/down/remove in `AdminOrderViewModel` keeps positions 1..n without gaps.

## 6. Done when
- [ ] New migration applies cleanly after the existing ones; `supabase/tests/run.sh` passes.
- [ ] Non-admins cannot change order (tested).
- [ ] Browsing a category shows admin order first, then newest; search still ranks by relevance.
- [ ] Admin screen works on a phone-size screen, all text in Tigrinya.
- [ ] `supabase/README.md` explains how to add the first admin and the new RPCs (Tigrinya + English, like the rest of the README).
- [ ] No unrelated changes; existing tests still pass.
