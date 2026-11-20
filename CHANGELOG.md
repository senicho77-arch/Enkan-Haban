# ለውጥታት / Changelog

## ከመይ ንሰርሕ / How we work
- ሓደ ጨንፈር ጥራይ: **`main`**። ኣብ Android Studio ኩሉ ግዜ `main` ሓዝ፡ ድሕሪኡ `git pull`።
- One branch only: **`main`**. In Android Studio always stay on `main` and use `git pull`.
- Every new change starts from the latest `main` and comes back into `main`. Each release gets a version
  number (in `androidApp/build.gradle.kts` and `iosApp/Configuration/Config.xcconfig`) and a git tag
  like `v1.0.1`. GitHub offers each tag as a zip download.

## 1.0.3
ስእሊ ማስታወቂያ ብኣድሚን ጥራይ / Advert banners by the admin only:
- 📢 The banners at the top of the home screen now come from the database (`promo_banners`) instead of being fixed in the app.
- Only admins can post, edit, hide, reorder or delete them — the database refuses everyone else (RLS + 10 new SQL tests).
- New admin screen "📢 ምሕደራ ማስታወቂያ" (Account tab, admins only) with a posting form: picture, title, text,
  button text, colour, what a tap opens (a category), start/end date, on/off, and a live preview.
- Banner pictures are stored in Supabase Storage under `listing-images/promos/` (admins only).
- New migration: `supabase/migrations/20261120000000_promo_banners.sql` — run it once in the SQL Editor.

## 1.0.2
ምስ Supabase ምትእስሳር / Connected to Supabase:
- 🔌 The app reads real posts and categories from Supabase when `SupabaseConfig.kt` has the project URL and anon key
  (empty = demo data, as before).
- 🔑 "እቶ ብ Google" works: Google sign-in in the browser, back to the app via `enkanhaban://login-callback`; "ውጻእ" signs out.
- 🖼️ Photos uploaded to Supabase Storage show on the cards and the product page.
- 🛠️ The admin order screen saves to the database (`admin_*` functions); "ምሕደራ መስርዕ" shows only for admins (`rpc("is_admin")`).
- Not yet: posting/editing items and photo upload (1.0.3); saved items and chat in the database (1.0.4).

## 1.0.1
ኣብዚ ስሪት ዘሎ ኩሉ / Everything in this version:
- 📞 ኣብ ኩሉ ኣቕሓ ቁጽሪ ተሌፎን ሸያጢ ብቐጥታ፡ WhatsApp፡ "ደውለለይ" (request call back) — no more add-to-cart.
- 💬 ምስ ሸያጢ ዕላል: quick messages (ዋጋ ኣቕርብ, ኣሎ ድዩ?, መወዳእታ ዋጋ) and "መልእኽታተይ" in Account.
- 🛒 ዘንቢል = ዝመረጽካዮም ኣቕሑ: save items with 🤍/❤️ and call the seller from the list.
- ✏️ ሸያጢ ንኣቕሑ ባዕሉ ከመሓይሽ ይኽእል (price, photos, text, details).
- 🛠️ ምሕደራ መስርዕ: the admin sets the order of posts in each category (home feed = "all").
- ⚠️ ናይ ውሕስነት መጠንቀቕታ ኣብ ኩሉ ኣቕሓ.
- Database: admin listing order + call back requests migrations, 92 SQL tests passing.
