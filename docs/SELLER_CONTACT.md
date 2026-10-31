# Seller contact on classified ads (Jiji-style)

> **ብትግርኛ (ንዋና):** እዚ ፋይል ንኮድ AI እዩ። ዓማዊል ንሸያጢ ብቐጥታ ዝረኽብሉ ክፋል ይገልጽ፦
> ቁጽሪ ተሌፎን ብቐጥታ ምርኣይ፡ ብሓንቲ ምጥዋቕ ምድዋል፡ WhatsApp፡ "ደውለለይ"፡ ቅልጡፍ መልእኽትታትን ዕላልን።
> እቲ ኮድ ኣብ repo ተጻሒፉ ኣሎ፡ ግን ኣብ ኮምፒተር ገና ኣይተሃንጸን (Android SDK ኣይነበረን)።
> ስለዚ ናይ AI ስራሕ፦ (1) ሃኒጹ ጌጋታት ይእርም፡ (2) ይፍትን፡ (3) ድሕሪ Google login ናብ Supabase የራኽቦ።
> ፋይሉ ኣብ AI ኣእትዮ ከምዚ በልዮ፦ "Follow docs/SELLER_CONTACT.md"።

---

## 1. Context

Repo: `senicho77-arch/Enkan-Haban` — Kotlin/Compose Multiplatform (Android + iOS, shared code in
`shared/src/commonMain/kotlin/com/example/enkan_haban`), backend Supabase (`supabase/`).
UI text is **Tigrinya** — keep it. Keep existing style; no unrelated refactors.

Two kinds of listing (`domain/Models.kt` → `ListingType`):
- `STORE` — bought through the cart (unchanged by this feature).
- `CLASSIFIED` — a customer's ad; buyers contact the seller directly. **Everything below is for CLASSIFIED only.**

## 2. What is already implemented (commits `d73bf07`, `960db2f` on `main`)

### 2.1 Product detail screen — `ui/product/ProductDetailScreen.kt`
Order on the screen for a classified ad:
1. Photo, title, price, location.
2. `SafetyWarning()` — red box: "⚠️ ኣቕሓ ብዓይንኻ ከይረኣኻ ገንዘብ ኣቐዲምካ ኣይትኽፈል — ንመብጽሒ (delivery) እውን።"
3. Description.
4. Seller card: name, ✔ verified, **phone number in bold**, and a green **"💬 WhatsApp"** button
   (only when the number has a country code — see `whatsappUri`).
5. `ChatWithSellerCard` — title "ምስ ሸያጢ ተዘራረብ":
   - Quick-reply chips (`SuggestionChip`) that fill the text box (buyer can edit before sending):
     | Chip | Text put in the box |
     |---|---|
     | ዋጋ ኣቕርብ (Make an offer) | ሰላም፡ ነዚ ኣቕሓ ብ… ክገዝኦ ደልየ። |
     | ኣሎ ድዩ? (Is this available) | ሰላም፡ እዚ ኣቕሓ ገና ኣሎ ድዩ? |
     | መወዳእታ ዋጋ (Last price) | ሰላም፡ መወዳእታ ዋጋኹም ክንደይ እዩ? |
   - Text field "መልእኽትኻ ኣብዚ ጽሓፍ" (max 4000 chars).
   - Orange button **"ዕላል ጀምር"** (Start chat) → `chatRepository.startChat(product, text)` → opens `Route.Chat(id)`.
6. Sticky bottom bar (`ClassifiedActions`):
   - Outlined **"ደውለለይ"** (Request call back) → `CallbackDialog` (name + phone, validated with
     `isValidPhone`) → `chatRepository.requestCallback(...)`. After sending, the button shows "✔ ተሓቲቱ" and is disabled.
   - Green button **"📞 <seller phone>"** — one tap opens the dialer via `LocalUriHandler.openUri(telUri(phone))`.
     The number is visible without any extra tap.

`ProductDetailScreen` now takes `onOpenChat: (conversationId: String) -> Unit`.

### 2.2 Helpers
- `util/Phone.kt`: `telUri(phone)` → `tel:+256…`; `whatsappUri(phone)` → `https://wa.me/<digits>` or
  `null` if the number doesn't start with `+` or `00` (we can't guess the country).
- `data/ChatRepository.kt`: `isValidPhone(phone)` (same regex as backend `^\+?[0-9 ]{7,20}$` + at least 9 digits),
  `MAX_MESSAGE_LENGTH = 4000`.

### 2.3 Chat (in memory for now)
- `domain/Models.kt`: `ChatMessage(id, body, fromMe)`, `Conversation(id, product, messages)`, `CallbackRequest(productId, name, phone)`.
- `data/ChatRepository.kt`: interface `ChatRepository` + `InMemoryChatRepository`
  (one conversation per ad, id `"chat-<productId>"`; newest activity first; asking for a call back
  again on the same ad replaces the earlier request). Registered as `AppContainer.chatRepository`.
- `ui/chat/ChatScreen.kt`:
  - `ChatScreen` — seller name in top bar, tappable product header (title + price → product page),
    message bubbles (mine on the right), note "ሸያጢ ምስ መለሰ፡ መልሱ ኣብዚ ክርአ እዩ።", input + "ስደድ".
  - `ChatListScreen` — "መልእኽታተይ", list of chats (product title, last message, seller name).
- Navigation (`ui/navigation/Navigator.kt`): `Route.Chat(conversationId)`, `Route.Chats`; wired in `App.kt`.
- `AccountScreen(onOpenChats)` has a clickable row "💬  መልእኽታተይ".

### 2.4 Sell form — `ui/sell/SellScreen.kt`
Phone field label "ቁጽሪ ተሌፎን (ምስ +256 …)" with hint that the number is shown to buyers and that
WhatsApp works when the country code is included.

### 2.5 Backend — `supabase/migrations/20261031000100_callback_requests.sql`
- Table `callback_requests` (listing_id, buyer_id, seller_id, buyer_name, buyer_phone, status `new|done`, created_at).
- `rpc request_callback(p_listing_id, p_name, p_phone)` — only signed-in users, only active classified ads,
  not your own ad; validates name/phone; one open request per buyer per ad (asking again updates it).
- RLS: buyer and seller can read; only the seller can update, and only `status`. No direct inserts.
- Chat already existed: `rpc start_conversation(listing_id)`, tables `conversations` / `messages`, realtime on `messages`.
- `contact_phone` on `listings` is public (shown on the ad).
- SQL tests in `supabase/tests/security_test.sql` — `supabase/tests/run.sh` → **59 passed, 0 failed**.

## 3. Your tasks

### Task 1 — Build and fix (do this first)
The Kotlin code was written **without compiling** (no Android SDK on that machine).
1. Run `./gradlew :androidApp:assembleDebug` and `./gradlew :shared:allTests` (or the host tests task).
2. Fix any compile errors with the smallest change. Likely spots: material3 opt-ins
   (`SuggestionChip`, `AlertDialog`, `Surface(onClick=…)` → add `@OptIn(ExperimentalMaterial3Api::class)` if required),
   `FlowRow` opt-in, missing imports, `return@Column` inside composables.
3. Don't change behaviour or Tigrinya texts while fixing.
4. Also build iOS if a Mac is available (`iosApp` in Xcode).

### Task 2 — Check on a phone (acceptance)
- [ ] Open the sample ad "iPhone 13 – 128GB, clean" (seller Sara K.): number visible on the green button;
      tap → dialer opens with `+256752556677`, no permission prompt.
- [ ] WhatsApp button opens `wa.me/256752556677`; hidden for a number like `0757 019 683`.
- [ ] Green button text fits on a ~360dp wide phone (shrink font or padding if cut off).
- [ ] "ደውለለይ": empty name / bad number show errors; valid → dialog closes, button becomes "✔ ተሓቲቱ".
- [ ] Quick chips fill the box; "ዕላል ጀምር" disabled when empty; sending opens the chat with the message.
- [ ] Sending again from the same ad goes into the same chat. Account → መልእኽታተይ lists it.
- [ ] Keyboard does not cover the chat input (`imePadding`).
- [ ] Store products (`STORE`) still show only the cart buttons — no phone, no chat, no warning.
- [ ] Existing tests still pass, including `sellerPhoneLinks`, `chatReopensSameConversationPerAd`,
      `callbackRequestValidatesAndReplaces` in `shared/src/commonTest/.../SharedCommonTest.kt`.

### Task 3 — Connect to Supabase (only after Google sign-in exists)
Keep the `ChatRepository` interface; add a `SupabaseChatRepository` and switch `AppContainer` to it.
- `startChat` → `rpc("start_conversation", listing_id)` then insert into `messages`.
- `conversations` → select own conversations ordered by `last_message_at desc`, join the listing.
- Incoming seller replies → subscribe to realtime inserts on `messages`; `fromMe = sender_id == auth.uid()`.
  Mark read with `update messages set read_at = now()`.
- `requestCallback` → `rpc("request_callback", p_listing_id, p_name, p_phone)`; prefill name/phone from `rpc("get_my_profile")`.
- Seller side (new): a "ደወልቲ" list (open `callback_requests` where `seller_id = me`) with a call button and "ተደዊሉ" (marks `status = 'done'`).
- If the buyer is not signed in, tapping "ደውለለይ" or "ዕላል ጀምር" should ask them to sign in first. Calling and WhatsApp work without sign-in.
- Show the listing's `contact_phone` on the ad (not the private `profiles.phone`).

## 4. Rules
- Don't remove the safety warning or hide the phone number behind an extra tap — the owner wants buyers to reach sellers directly.
- Don't make phone numbers from `profiles` public; only `listings.contact_phone` is public.
- Any new SQL goes in a new migration file with tests; never edit old migrations.
