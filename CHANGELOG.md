# ለውጥታት / Changelog

## ከመይ ንሰርሕ / How we work
- ሓደ ጨንፈር ጥራይ: **`main`**። ኣብ Android Studio ኩሉ ግዜ `main` ሓዝ፡ ድሕሪኡ `git pull`።
- One branch only: **`main`**. In Android Studio always stay on `main` and use `git pull`.
- Every new change starts from the latest `main` and comes back into `main`. Each release gets a version
  number (in `androidApp/build.gradle.kts` and `iosApp/Configuration/Config.xcconfig`) and a git tag
  like `v1.0.1`. GitHub offers each tag as a zip download.

## 1.0.1
ኣብዚ ስሪት ዘሎ ኩሉ / Everything in this version:
- 📞 ኣብ ኩሉ ኣቕሓ ቁጽሪ ተሌፎን ሸያጢ ብቐጥታ፡ WhatsApp፡ "ደውለለይ" (request call back) — no more add-to-cart.
- 💬 ምስ ሸያጢ ዕላል: quick messages (ዋጋ ኣቕርብ, ኣሎ ድዩ?, መወዳእታ ዋጋ) and "መልእኽታተይ" in Account.
- 🛒 ዘንቢል = ዝመረጽካዮም ኣቕሑ: save items with 🤍/❤️ and call the seller from the list.
- ✏️ ሸያጢ ንኣቕሑ ባዕሉ ከመሓይሽ ይኽእል (price, photos, text, details).
- 🛠️ ምሕደራ መስርዕ: the admin sets the order of posts in each category (home feed = "all").
- ⚠️ ናይ ውሕስነት መጠንቀቕታ ኣብ ኩሉ ኣቕሓ.
- Database: admin listing order + call back requests migrations, 92 SQL tests passing.
