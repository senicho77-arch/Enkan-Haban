# እንካን ሃባን — ዳታበይዝ (Supabase / PostgreSQL)

## ኣብዚ ዘሎ
| ፋይል | እንታይ ይገብር |
|---|---|
| `migrations/20261027000000_init_marketplace.sql` | 14 ሰሌዳታት, ፍተሻ (search), ትእዛዝ (place_order), ዕላል, ውሕስነት (RLS) |
| `migrations/20261027000100_storage.sql` | ስእልታት ንብረት ዝቕመጣሉ ቦታ (`listing-images`) |
| `migrations/20261027000200_reference_data.sql` | ባጤራታት (UGX, ETB, ERN, USD, EUR…) ን ምድብታት ብትግርኛን እንግሊዝኛን |
| `migrations/20261028000000_hardening.sql` | ውሕስነት ምድልዳል — security fixes found by testing (see below) |
| `migrations/20261031000000_admin_listing_order.sql` | ኣድሚን መስርዕ ፖስትታት ኣብ ነፍሲ ወከፍ ምድብ ይውስን — admin-controlled listing order per category |
| `tests/` | 80 tests that act as real users (`tests/run.sh`) |

## ሰሌዳታት (tables)
- **profiles** — ተጠቀምቲ (ብ Google ምስ ኣተዉ ባዕሉ ይፍጠር)
- **stores** — ድኳናት · **categories** — ምድብታት (ti/en)
- **listings** — ንብረት (store = ብዘንቢል ዝዕደግ, classified = መወዓውዒ) · **listing_images**
- **cart_items** — ዘንቢል · **orders / order_items** — ትእዛዛት
- **favorites** · **conversations / messages** — ዕላል ዓዳግን ሻያጥን
- **reviews** — ርእይቶ ሻያጢ · **reports** — ጸብጻብ ምትላል
- **currencies** — ባጤራታት
- **admins** — ኣድሚናት (ካብ ኣፕ ክንበብ ወይ ክቕየር ኣይክእልን) · **category_placements** — መስርዕ ፖስትታት ብኣድሚን

## ከመይ ትጥቀመሉ
1. ኣብ https://supabase.com ሓድሽ project ክፈት።
2. **Authentication → Providers → Google** ኣበርክቶ (Google Client ID/Secret ካብ Google Cloud)።
3. **SQL Editor** ክፈት፣ ነቶም ፋይላት ብቕደም ስዕቦም (000000 → 000100 → 000200 → 20261028000000 → 20261031000000) ለጢፍካ **Run** በል።
   ወይ ብ Supabase CLI: `supabase link` ድሕሪኡ `supabase db push`።

## ውሕስነት (ዝተፈተነ)
- ሓደ ተጠቀምቲ ናይ ካልእ ሰብ ንብረት ክቕይር ወይ ክድምስስ ኣይክእልን።
- ዓዳጊ ዋጋ ትእዛዝ ክቕይር ኣይክእልን — ዋጋ ካብ ዳታበይዝ ይንበብ (`place_order`)።
- ዘንቢል ንድኳን ንብረት ጥራይ; መወዓውዒ (classified) ብዕላል ወይ ብቴሌፎን።
- `is_verified` / `is_official` ብኣድሚን ጥራይ ይቕየሩ።
- ዕላል ክልቲኦም ተሳተፍቲ ጥራይ ይርእይዎ።
- Phone numbers in `profiles` are private; listings can't be posted with fake view counts.
- Only admins (service role / SQL editor) can set a listing to `removed` or bring it back.
- Orders only move forward (pending → confirmed → shipped → delivered); cancelling restores stock.
- One review per buyer per seller per listing, and only after a chat or an order.

## ኣድሚን / Admins and the listing order
ቀዳማይ ኣድሚን (ንስኻ/ንስኺ) ብ SQL Editor ጥራይ ይውሰኽ — ካብ ኣፕ ኣይከኣልን።
The first admin is added by the owner in the Supabase **SQL Editor** (sign in to the app once with Google first,
then copy your user id from **Authentication → Users**):
```sql
insert into public.admins (user_id) values ('<owner auth user id>');
```
ኣድሚን ኣብ ነፍሲ ወከፍ ምድብ ፖስትታት ብዝደለዮ መስርዕ ይሰርዕ። ዝተሰርዑ ቀዲሞም ይርኣዩ፡ ካልኦት ድሕሪኦም ብሓድሽ-ቅድሚት።
- Every category (top and sub-category) has its own order; `'all'` is the home feed with no category.
- Placed listings come first (position 1, 2, 3 …), all others follow newest first.
- `pinned_until` (optional): after that time the placement is ignored automatically — no cron job.
- Placements of listings that are sold, paused, removed or moved to another category are ignored.
- With a search text, relevance wins; the admin order is only a tiebreaker.
- Only admins can change placements. Normal users can't read `admins` and every `admin_*` call fails for them.

| Need (admin only) | Call |
|---|---|
| Am I admin? | `rpc("is_admin")` |
| Replace a category's whole order | `rpc("admin_set_category_order", p_category_id, p_listing_ids)` — empty array clears it; keeps existing `pinned_until` |
| Put one listing at a position | `rpc("admin_pin_listing", p_category_id, p_listing_id, p_position = 1, p_pinned_until = null)` |
| Take one listing out | `rpc("admin_unpin_listing", p_category_id, p_listing_id)` |
| Show the order (admin screen) | `rpc("admin_get_category_order", p_category_id)` → id, title, position, pinned_until, is_expired |

## ንኣፕ / Notes for the app
| Need | Call |
|---|---|
| Feed / search | `rpc("search_listings", q, p_category_id, p_country, p_city, p_type, p_limit, p_offset)` — admin order first when browsing |
| Own profile (incl. phone, settings) | `rpc("get_my_profile")` — `profiles` itself only shows the public card, so select columns explicitly (`id, full_name, avatar_url, country_code, city, is_verified, created_at`), not `*` |
| Checkout | `rpc("place_order", …)` — one call per store in the cart |
| Buyer cancels (while pending) | `rpc("cancel_order", p_order_id)` |
| Store moves order on | `update orders set status = …` (confirmed → shipped → delivered, or cancelled before shipping) |
| Open a chat | `rpc("start_conversation", p_listing_id)`; new messages arrive live via Realtime on `messages` |
| Count a view | `rpc("increment_listing_view", p_listing_id)` |

## ፈተና / Tests
Needs a local PostgreSQL 15+ (no Supabase needed — `tests/supabase_stub.sql` stands in for `auth` and `storage`):
```bash
PGHOST=localhost PGUSER=postgres supabase/tests/run.sh
```

ዋጋታት ብ "minor units" እዮም (ከም `Product.price` ኣብ ኣፕ)፥ USD 12.50 = `1250`, UGX 45,000 = `45000`.
