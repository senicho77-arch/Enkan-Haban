-- =============================================================================
-- እንካን ሃባን (Enkan Haban) — ናይ ዳታበይዝ መሰረት / database foundation
-- Supabase (PostgreSQL). Run with:  supabase db push   (or paste into the SQL editor)
--
-- Main ideas
--   * Every user signs in with Google (Supabase Auth). A row in public.profiles is
--     created automatically for each new user.
--   * Two ways to sell (same as the app's ListingType):
--       store      -> bought through the cart and checked out (Jumia-style)
--       classified -> an ad; the buyer chats with / calls the seller (Jiji-style)
--   * Prices are whole numbers in the currency's minor unit (cents), like the app's
--     Product.price. Each listing says which currency it is in (UGX, ETB, USD, EUR...).
--   * Names that users see (categories, currencies) are stored in Tigrinya AND English.
--   * Row Level Security (RLS) is on for every table: users can only change their own data.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Enum types
-- ---------------------------------------------------------------------------
create type public.listing_type      as enum ('store', 'classified');
create type public.item_condition    as enum ('new', 'used', 'refurbished');
create type public.listing_status    as enum ('draft', 'active', 'paused', 'sold', 'removed');
create type public.order_status      as enum ('pending', 'confirmed', 'shipped', 'delivered', 'cancelled');
create type public.payment_method    as enum ('cash_on_delivery', 'mobile_money', 'card');
create type public.app_language      as enum ('ti', 'en', 'am');
create type public.report_reason     as enum ('scam', 'prohibited_item', 'wrong_category', 'offensive', 'other');

-- ---------------------------------------------------------------------------
-- Helper: keep updated_at current
-- ---------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

-- ---------------------------------------------------------------------------
-- ባጤራታት / Currencies
-- ---------------------------------------------------------------------------
create table public.currencies (
  code        char(3) primary key,                 -- ISO 4217, e.g. 'UGX'
  name_ti     text not null,
  name_en     text not null,
  symbol      text not null,
  minor_units smallint not null check (minor_units between 0 and 3)  -- UGX = 0, USD = 2
);

-- ---------------------------------------------------------------------------
-- ተጠቀምቲ / Profiles (one per signed-in Google user)
-- ---------------------------------------------------------------------------
create table public.profiles (
  id                 uuid primary key references auth.users (id) on delete cascade,
  full_name          text not null default '' check (char_length(full_name) <= 120),
  avatar_url         text,
  phone              text check (phone is null or phone ~ '^\+?[0-9 ]{7,20}$'),
  country_code       char(2),                       -- ISO 3166, e.g. 'UG', 'ET', 'ER', 'CA'
  city               text,
  preferred_language public.app_language not null default 'ti',
  preferred_currency char(3) references public.currencies (code),
  is_verified        boolean not null default false, -- set by admins only
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create trigger profiles_updated_at before update on public.profiles
  for each row execute function public.set_updated_at();

-- Create the profile automatically when someone signs in with Google for the first time.
-- Google puts the name and photo into raw_user_meta_data.
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles (id, full_name, avatar_url)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name', ''),
    coalesce(new.raw_user_meta_data ->> 'avatar_url', new.raw_user_meta_data ->> 'picture')
  );
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- ድኳናት / Stores (a seller's shop that sells through the cart)
-- ---------------------------------------------------------------------------
create table public.stores (
  id           uuid primary key default gen_random_uuid(),
  owner_id     uuid not null references public.profiles (id) on delete cascade,
  name         text not null check (char_length(name) between 2 and 80),
  slug         text not null unique check (slug ~ '^[a-z0-9-]{3,40}$'),
  description  text check (char_length(description) <= 2000),
  logo_url     text,
  phone        text,
  country_code char(2) not null,
  city         text not null,
  is_verified  boolean not null default false,   -- set by admins only
  is_official  boolean not null default false,   -- "EnkanHaban Store" itself
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);
create index stores_owner_idx on public.stores (owner_id);
create trigger stores_updated_at before update on public.stores
  for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- ምድብታት / Categories (Tigrinya + English, can have sub-categories)
-- ---------------------------------------------------------------------------
create table public.categories (
  id         text primary key check (id ~ '^[a-z0-9-]+$'),  -- e.g. 'phones'
  parent_id  text references public.categories (id) on delete restrict,
  name_ti    text not null,
  name_en    text not null,
  icon       text,                                         -- emoji, as in the app today
  sort_order smallint not null default 0,
  is_active  boolean not null default true
);
create index categories_parent_idx on public.categories (parent_id);

-- ---------------------------------------------------------------------------
-- ንብረት ኣብ ዕዳጋ / Listings (= Product in the app)
-- ---------------------------------------------------------------------------
create table public.listings (
  id              uuid primary key default gen_random_uuid(),
  seller_id       uuid not null references public.profiles (id) on delete cascade,
  store_id        uuid references public.stores (id) on delete cascade,
  category_id     text not null references public.categories (id),
  listing_type    public.listing_type not null,
  status          public.listing_status not null default 'active',
  title           text not null check (char_length(title) between 3 and 120),
  description     text not null default '' check (char_length(description) <= 5000),
  condition       public.item_condition not null default 'new',
  price_minor     bigint not null check (price_minor >= 0),
  old_price_minor bigint check (old_price_minor is null or old_price_minor > price_minor),
  currency        char(3) not null references public.currencies (code),
  is_negotiable   boolean not null default false,
  stock_quantity  integer check (stock_quantity is null or stock_quantity >= 0),
  country_code    char(2) not null,
  city            text not null,
  area            text,                              -- e.g. 'Kabalagala', 'Bole'
  contact_phone   text,                              -- shown on classified ads
  view_count      integer not null default 0,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  -- Search across Tigrinya and English text ('simple' = no language-specific stemming)
  search tsvector generated always as (
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(city, '') || ' ' || coalesce(area, '')), 'C')
  ) stored,
  -- store items belong to a store and track stock; classified ads have neither
  constraint listing_type_rules check (
    (listing_type = 'store'      and store_id is not null and stock_quantity is not null) or
    (listing_type = 'classified' and store_id is null)
  )
);
create index listings_feed_idx     on public.listings (status, created_at desc);
create index listings_category_idx on public.listings (category_id, status, created_at desc);
create index listings_place_idx    on public.listings (country_code, city);
create index listings_seller_idx   on public.listings (seller_id);
create index listings_store_idx    on public.listings (store_id);
create index listings_search_idx   on public.listings using gin (search);
create trigger listings_updated_at before update on public.listings
  for each row execute function public.set_updated_at();

-- A store listing must be posted by the store's owner.
create or replace function public.check_listing_store_owner()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.store_id is not null and not exists (
    select 1 from public.stores s where s.id = new.store_id and s.owner_id = new.seller_id
  ) then
    raise exception 'store % does not belong to seller %', new.store_id, new.seller_id;
  end if;
  return new;
end $$;
create trigger listings_store_owner before insert or update of store_id, seller_id on public.listings
  for each row execute function public.check_listing_store_owner();

-- ---------------------------------------------------------------------------
-- ስእልታት / Listing images (files live in Storage bucket 'listing-images')
-- ---------------------------------------------------------------------------
create table public.listing_images (
  id           uuid primary key default gen_random_uuid(),
  listing_id   uuid not null references public.listings (id) on delete cascade,
  storage_path text not null,               -- '<user id>/<listing id>/<file>.jpg'
  sort_order   smallint not null default 0, -- 0 = cover photo
  created_at   timestamptz not null default now(),
  unique (listing_id, sort_order)
);

-- ---------------------------------------------------------------------------
-- ዝፈተኹዎም / Favorites
-- ---------------------------------------------------------------------------
create table public.favorites (
  user_id    uuid not null references public.profiles (id) on delete cascade,
  listing_id uuid not null references public.listings (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, listing_id)
);

-- ---------------------------------------------------------------------------
-- ዘንቢል / Cart (store listings only — same rule as CartRepository.add)
-- ---------------------------------------------------------------------------
create table public.cart_items (
  user_id    uuid not null references public.profiles (id) on delete cascade,
  listing_id uuid not null references public.listings (id) on delete cascade,
  quantity   integer not null check (quantity between 1 and 99),
  added_at   timestamptz not null default now(),
  primary key (user_id, listing_id)
);

create or replace function public.check_cart_item()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if not exists (
    select 1 from public.listings l
    where l.id = new.listing_id and l.listing_type = 'store' and l.status = 'active'
  ) then
    raise exception 'only active store listings can be added to the cart';
  end if;
  return new;
end $$;
create trigger cart_items_check before insert or update on public.cart_items
  for each row execute function public.check_cart_item();

-- ---------------------------------------------------------------------------
-- ትእዛዛት / Orders (one order per store + currency; created only by place_order)
-- ---------------------------------------------------------------------------
create table public.orders (
  id                 uuid primary key default gen_random_uuid(),
  buyer_id           uuid not null references public.profiles (id) on delete restrict,
  store_id           uuid not null references public.stores (id) on delete restrict,
  status             public.order_status not null default 'pending',
  payment_method     public.payment_method not null default 'cash_on_delivery',
  currency           char(3) not null references public.currencies (code),
  subtotal_minor     bigint not null check (subtotal_minor >= 0),
  delivery_fee_minor bigint not null default 0 check (delivery_fee_minor >= 0),
  total_minor        bigint generated always as (subtotal_minor + delivery_fee_minor) stored,
  recipient_name     text not null,
  recipient_phone    text not null,
  country_code       char(2) not null,
  city               text not null,
  address_line       text not null,
  note               text,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create index orders_buyer_idx on public.orders (buyer_id, created_at desc);
create index orders_store_idx on public.orders (store_id, status, created_at desc);
create trigger orders_updated_at before update on public.orders
  for each row execute function public.set_updated_at();

create table public.order_items (
  id               bigint generated always as identity primary key,
  order_id        uuid not null references public.orders (id) on delete cascade,
  listing_id       uuid references public.listings (id) on delete set null,
  title_snapshot   text not null,   -- keep the title/price as they were when bought
  unit_price_minor bigint not null check (unit_price_minor >= 0),
  quantity         integer not null check (quantity > 0),
  line_total_minor bigint generated always as (unit_price_minor * quantity) stored
);
create index order_items_order_idx on public.order_items (order_id);

-- ---------------------------------------------------------------------------
-- ዕላል / Chat between buyer and seller about a listing
-- ---------------------------------------------------------------------------
create table public.conversations (
  id              uuid primary key default gen_random_uuid(),
  listing_id      uuid not null references public.listings (id) on delete cascade,
  buyer_id        uuid not null references public.profiles (id) on delete cascade,
  seller_id       uuid not null references public.profiles (id) on delete cascade,
  last_message_at timestamptz not null default now(),
  created_at      timestamptz not null default now(),
  unique (listing_id, buyer_id),
  check (buyer_id <> seller_id)
);
create index conversations_buyer_idx  on public.conversations (buyer_id, last_message_at desc);
create index conversations_seller_idx on public.conversations (seller_id, last_message_at desc);

create table public.messages (
  id              bigint generated always as identity primary key,
  conversation_id uuid not null references public.conversations (id) on delete cascade,
  sender_id       uuid not null references public.profiles (id) on delete cascade,
  body            text not null check (char_length(body) between 1 and 4000),
  created_at      timestamptz not null default now(),
  read_at         timestamptz
);
create index messages_conversation_idx on public.messages (conversation_id, created_at);

create or replace function public.bump_conversation()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  update public.conversations set last_message_at = new.created_at where id = new.conversation_id;
  return new;
end $$;
create trigger messages_bump after insert on public.messages
  for each row execute function public.bump_conversation();

-- ---------------------------------------------------------------------------
-- ርእይቶታት / Reviews of sellers
-- ---------------------------------------------------------------------------
create table public.reviews (
  id          uuid primary key default gen_random_uuid(),
  reviewer_id uuid not null references public.profiles (id) on delete cascade,
  seller_id   uuid not null references public.profiles (id) on delete cascade,
  listing_id  uuid references public.listings (id) on delete set null,
  rating      smallint not null check (rating between 1 and 5),
  comment     text check (char_length(comment) <= 2000),
  created_at  timestamptz not null default now(),
  unique (reviewer_id, seller_id, listing_id),
  check (reviewer_id <> seller_id)
);
create index reviews_seller_idx on public.reviews (seller_id, created_at desc);

-- Average rating per seller (the app's Product.rating / seller badge)
create view public.seller_ratings with (security_invoker = true) as
  select seller_id, round(avg(rating)::numeric, 1) as average_rating, count(*) as review_count
  from public.reviews group by seller_id;

-- ---------------------------------------------------------------------------
-- ጸብጻብ / Reports (users flag scams or bad listings; admins read them)
-- ---------------------------------------------------------------------------
create table public.reports (
  id          bigint generated always as identity primary key,
  reporter_id uuid not null references public.profiles (id) on delete cascade,
  listing_id  uuid references public.listings (id) on delete cascade,
  user_id     uuid references public.profiles (id) on delete cascade,
  reason      public.report_reason not null,
  details     text check (char_length(details) <= 2000),
  created_at  timestamptz not null default now(),
  check (listing_id is not null or user_id is not null)
);

-- =============================================================================
-- Functions the app calls (supabase.postgrest.rpc(...))
-- =============================================================================

-- Search the feed. Every argument is optional.
create or replace function public.search_listings(
  q             text default null,
  p_category_id text default null,
  p_country     char(2) default null,
  p_city        text default null,
  p_type        public.listing_type default null,
  p_limit       int default 30,
  p_offset      int default 0
) returns setof public.listings
language sql stable as $$
  select l.*
  from public.listings l
  where l.status = 'active'
    and (p_category_id is null or l.category_id = p_category_id
         or l.category_id in (select c.id from public.categories c where c.parent_id = p_category_id))
    and (p_country is null or l.country_code = p_country)
    and (p_city    is null or l.city ilike p_city)
    and (p_type    is null or l.listing_type = p_type)
    and (coalesce(trim(q), '') = ''
         or l.search @@ websearch_to_tsquery('simple', q)
         or l.title ilike '%' || q || '%')
  order by
    case when coalesce(trim(q), '') = '' then 0
         else ts_rank(l.search, websearch_to_tsquery('simple', q)) end desc,
    l.created_at desc
  limit least(p_limit, 100) offset p_offset
$$;

-- Turn the signed-in user's cart items from one store into an order.
-- Prices are read from the database, so a buyer cannot change what they pay.
create or replace function public.place_order(
  p_store_id        uuid,
  p_recipient_name  text,
  p_recipient_phone text,
  p_country_code    char(2),
  p_city            text,
  p_address_line    text,
  p_payment_method  public.payment_method default 'cash_on_delivery',
  p_note            text default null
) returns uuid
language plpgsql security definer set search_path = '' as $$
declare
  v_buyer    uuid := auth.uid();
  v_currency char(3);
  v_order    uuid;
  v_item     record;
  v_subtotal bigint := 0;
begin
  if v_buyer is null then
    raise exception 'not signed in';
  end if;

  -- lock the listings so two buyers can't take the last item at the same time
  for v_item in
    select l.id, l.title, l.price_minor, l.currency, l.stock_quantity, ci.quantity
    from public.cart_items ci
    join public.listings l on l.id = ci.listing_id
    where ci.user_id = v_buyer and l.store_id = p_store_id
    for update of l
  loop
    if v_currency is null then
      v_currency := v_item.currency;
    elsif v_currency <> v_item.currency then
      raise exception 'cart items from one store must use one currency';
    end if;
    if v_item.stock_quantity < v_item.quantity then
      raise exception 'not enough stock for "%"', v_item.title;
    end if;
    v_subtotal := v_subtotal + v_item.price_minor * v_item.quantity;
  end loop;

  if v_currency is null then
    raise exception 'cart has no items from this store';
  end if;

  insert into public.orders (buyer_id, store_id, payment_method, currency, subtotal_minor,
                             recipient_name, recipient_phone, country_code, city, address_line, note)
  values (v_buyer, p_store_id, p_payment_method, v_currency, v_subtotal,
          p_recipient_name, p_recipient_phone, p_country_code, p_city, p_address_line, p_note)
  returning id into v_order;

  insert into public.order_items (order_id, listing_id, title_snapshot, unit_price_minor, quantity)
  select v_order, l.id, l.title, l.price_minor, ci.quantity
  from public.cart_items ci join public.listings l on l.id = ci.listing_id
  where ci.user_id = v_buyer and l.store_id = p_store_id;

  update public.listings l
  set stock_quantity = l.stock_quantity - ci.quantity,
      status = case when l.stock_quantity - ci.quantity = 0 then 'sold'::public.listing_status else l.status end
  from public.cart_items ci
  where ci.user_id = v_buyer and ci.listing_id = l.id and l.store_id = p_store_id;

  delete from public.cart_items ci
  using public.listings l
  where ci.user_id = v_buyer and ci.listing_id = l.id and l.store_id = p_store_id;

  return v_order;
end $$;

-- Start (or reopen) a chat with the seller of a listing.
create or replace function public.start_conversation(p_listing_id uuid)
returns uuid
language plpgsql security definer set search_path = '' as $$
declare
  v_buyer  uuid := auth.uid();
  v_seller uuid;
  v_id     uuid;
begin
  if v_buyer is null then raise exception 'not signed in'; end if;
  select seller_id into v_seller from public.listings where id = p_listing_id and status = 'active';
  if v_seller is null then raise exception 'listing not found'; end if;
  if v_seller = v_buyer then raise exception 'you cannot message yourself'; end if;

  insert into public.conversations (listing_id, buyer_id, seller_id)
  values (p_listing_id, v_buyer, v_seller)
  on conflict (listing_id, buyer_id) do update set listing_id = excluded.listing_id
  returning id into v_id;
  return v_id;
end $$;

-- Count a view of a listing (anyone can call it; it only adds 1).
create or replace function public.increment_listing_view(p_listing_id uuid)
returns void
language sql security definer set search_path = '' as $$
  update public.listings set view_count = view_count + 1
  where id = p_listing_id and status = 'active';
$$;

-- =============================================================================
-- Row Level Security — ውሕስነት
-- =============================================================================
alter table public.currencies     enable row level security;
alter table public.profiles       enable row level security;
alter table public.stores         enable row level security;
alter table public.categories     enable row level security;
alter table public.listings       enable row level security;
alter table public.listing_images enable row level security;
alter table public.favorites      enable row level security;
alter table public.cart_items     enable row level security;
alter table public.orders         enable row level security;
alter table public.order_items    enable row level security;
alter table public.conversations  enable row level security;
alter table public.messages       enable row level security;
alter table public.reviews        enable row level security;
alter table public.reports        enable row level security;

-- Public reference data
create policy "everyone reads currencies" on public.currencies for select using (true);
create policy "everyone reads categories" on public.categories for select using (true);

-- Profiles: public cards, each user edits only their own
create policy "everyone reads profiles"   on public.profiles for select using (true);
create policy "users update own profile"  on public.profiles for update
  using (id = auth.uid()) with check (id = auth.uid());

-- Stores
create policy "everyone reads stores"     on public.stores for select using (true);
create policy "users create own store"    on public.stores for insert with check (owner_id = auth.uid());
create policy "owners update store"       on public.stores for update
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "owners delete store"       on public.stores for delete using (owner_id = auth.uid());

-- Listings: active ones are public; sellers also see their own drafts/sold items
create policy "read active or own listings" on public.listings for select
  using (status = 'active' or seller_id = auth.uid());
create policy "sellers create listings"     on public.listings for insert with check (seller_id = auth.uid());
create policy "sellers update listings"     on public.listings for update
  using (seller_id = auth.uid()) with check (seller_id = auth.uid());
create policy "sellers delete listings"     on public.listings for delete using (seller_id = auth.uid());

-- Images follow their listing
create policy "read images of visible listings" on public.listing_images for select
  using (exists (select 1 from public.listings l where l.id = listing_id));
create policy "sellers add images" on public.listing_images for insert
  with check (exists (select 1 from public.listings l where l.id = listing_id and l.seller_id = auth.uid()));
create policy "sellers change images" on public.listing_images for update
  using (exists (select 1 from public.listings l where l.id = listing_id and l.seller_id = auth.uid()));
create policy "sellers remove images" on public.listing_images for delete
  using (exists (select 1 from public.listings l where l.id = listing_id and l.seller_id = auth.uid()));

-- Favorites and cart: private to each user
create policy "own favorites" on public.favorites for all
  using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "own cart" on public.cart_items for all
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Orders: buyer and store owner can see; only the store owner changes status
create policy "buyer or store sees order" on public.orders for select
  using (buyer_id = auth.uid()
         or exists (select 1 from public.stores s where s.id = store_id and s.owner_id = auth.uid()));
create policy "store owner updates order" on public.orders for update
  using (exists (select 1 from public.stores s where s.id = store_id and s.owner_id = auth.uid()));
create policy "see items of visible orders" on public.order_items for select
  using (exists (select 1 from public.orders o where o.id = order_id));

-- Chat: only the two people in the conversation
create policy "participants see conversation" on public.conversations for select
  using (buyer_id = auth.uid() or seller_id = auth.uid());
create policy "participants read messages" on public.messages for select
  using (exists (select 1 from public.conversations c where c.id = conversation_id
                 and (c.buyer_id = auth.uid() or c.seller_id = auth.uid())));
create policy "participants send messages" on public.messages for insert
  with check (sender_id = auth.uid()
              and exists (select 1 from public.conversations c where c.id = conversation_id
                          and (c.buyer_id = auth.uid() or c.seller_id = auth.uid())));
create policy "recipient marks read" on public.messages for update
  using (sender_id <> auth.uid()
         and exists (select 1 from public.conversations c where c.id = conversation_id
                     and (c.buyer_id = auth.uid() or c.seller_id = auth.uid())));

-- Reviews: public; you write your own
create policy "everyone reads reviews" on public.reviews for select using (true);
create policy "users write reviews"    on public.reviews for insert with check (reviewer_id = auth.uid());
create policy "users edit own reviews" on public.reviews for update using (reviewer_id = auth.uid());
create policy "users delete own reviews" on public.reviews for delete using (reviewer_id = auth.uid());

-- Reports: users can send; only admins (service role) read them
create policy "users send reports" on public.reports for insert with check (reporter_id = auth.uid());

-- =============================================================================
-- Column permissions: users must not set admin-only fields, prices on orders, etc.
-- (Supabase grants full access to 'anon' and 'authenticated' by default; RLS above
--  decides WHICH rows, the grants below decide WHICH columns.)
-- =============================================================================
revoke insert, update on public.profiles from anon, authenticated;
grant update (full_name, avatar_url, phone, country_code, city, preferred_language, preferred_currency)
  on public.profiles to authenticated;

revoke insert, update on public.stores from anon, authenticated;
grant insert (owner_id, name, slug, description, logo_url, phone, country_code, city)
  on public.stores to authenticated;
grant update (name, description, logo_url, phone, country_code, city)
  on public.stores to authenticated;

revoke insert, update, delete on public.currencies, public.categories from anon, authenticated;

revoke update on public.listings from anon, authenticated;
grant update (category_id, status, title, description, condition, price_minor, old_price_minor, currency,
              is_negotiable, stock_quantity, country_code, city, area, contact_phone)
  on public.listings to authenticated;

revoke insert, update, delete on public.orders, public.order_items from anon, authenticated;
grant update (status) on public.orders to authenticated;

revoke insert, update, delete on public.conversations from anon, authenticated;
revoke update on public.messages from anon, authenticated;
grant update (read_at) on public.messages to authenticated;

revoke execute on function public.place_order, public.start_conversation from public, anon;
grant  execute on function public.place_order, public.start_conversation to authenticated;
