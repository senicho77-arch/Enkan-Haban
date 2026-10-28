# እንካን ሃባን — ዳታበይዝ (Supabase / PostgreSQL)

## ኣብዚ ዘሎ
| ፋይል | እንታይ ይገብር |
|---|---|
| `migrations/20261027000000_init_marketplace.sql` | 14 ሰሌዳታት, ፍተሻ (search), ትእዛዝ (place_order), ዕላል, ውሕስነት (RLS) |
| `migrations/20261027000100_storage.sql` | ስእልታት ንብረት ዝቕመጣሉ ቦታ (`listing-images`) |
| `migrations/20261027000200_reference_data.sql` | ባጤራታት (UGX, ETB, ERN, USD, EUR…) ን ምድብታት ብትግርኛን እንግሊዝኛን |
| `migrations/20261028000000_hardening.sql` | ውሕስነት ምድልዳል — security fixes found by testing (see below) |
| `tests/` | 47 tests that act as real users (`tests/run.sh`) |

## ሰሌዳታት (tables)
- **profiles** — ተጠቀምቲ (ብ Google ምስ ኣተዉ ባዕሉ ይፍጠር)
- **stores** — ድኳናት · **categories** — ምድብታት (ti/en)
- **listings** — ንብረት (store = ብዘንቢል ዝዕደግ, classified = መወዓውዒ) · **listing_images**
- **cart_items** — ዘንቢል · **orders / order_items** — ትእዛዛት
- **favorites** · **conversations / messages** — ዕላል ዓዳግን ሻያጥን
- **reviews** — ርእይቶ ሻያጢ · **reports** — ጸብጻብ ምትላል
- **currencies** — ባጤራታት

## ከመይ ትጥቀመሉ
1. ኣብ https://supabase.com ሓድሽ project ክፈት።
2. **Authentication → Providers → Google** ኣበርክቶ (Google Client ID/Secret ካብ Google Cloud)።
3. **SQL Editor** ክፈት፣ ነቶም ኣርባዕተ ፋይላት ብቕደም ስዕቦም (000000 → 000100 → 000200 → 20261028000000) ለጢፍካ **Run** በል።
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

## ንኣፕ / Notes for the app
| Need | Call |
|---|---|
| Feed / search | `rpc("search_listings", q, p_category_id, p_country, p_city, p_type, p_limit, p_offset)` |
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
