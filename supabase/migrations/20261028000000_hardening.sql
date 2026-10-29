-- =============================================================================
-- ውሕስነት ምድልዳል / Hardening (fixes found by testing the first migrations)
--   1. Listings: users can no longer fake view_count / created_at on insert.
--   2. Moderation: only admins can remove a listing or bring a removed one back.
--   3. place_order refuses items that are no longer active (paused, sold...).
--   4. Orders move forward only (pending -> confirmed -> shipped -> delivered);
--      cancelling puts the stock back. Buyers can cancel while still pending.
--   5. Reviews: one per buyer per seller per listing (also when listing is empty),
--      only after a chat or an order, and only rating/comment can be edited.
--   6. Phone numbers and settings in profiles are private to their owner.
--   7. Listing images must point into the seller's own storage folder.
--   8. search_listings: safe limit/offset, and % or _ in the search text are literal.
--   9. Chat messages are sent live to the app (Supabase Realtime).
-- "Admin" below means the service role or the SQL editor — never a normal app user.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Listings: which columns a user may fill in on insert
-- ---------------------------------------------------------------------------
revoke insert on public.listings from anon, authenticated;
grant insert (id, seller_id, store_id, category_id, listing_type, status, title, description, condition,
              price_minor, old_price_minor, currency, is_negotiable, stock_quantity, country_code, city,
              area, contact_phone)
  on public.listings to authenticated;

-- ---------------------------------------------------------------------------
-- 2. Moderation: 'removed' is set and cleared by admins only
-- ---------------------------------------------------------------------------
-- Not security definer on purpose: current_user is then the caller's role.
create or replace function public.check_listing_moderation()
returns trigger language plpgsql set search_path = '' as $$
begin
  if current_user in ('anon', 'authenticated') and (
       (new.status = 'removed' and (tg_op = 'INSERT' or old.status <> 'removed')) or
       (tg_op = 'UPDATE' and old.status = 'removed' and new.status <> 'removed')
     ) then
    raise exception 'only moderators can remove a listing or restore a removed one';
  end if;
  return new;
end $$;
create trigger listings_moderation before insert or update of status on public.listings
  for each row execute function public.check_listing_moderation();

-- ---------------------------------------------------------------------------
-- 3. place_order: every cart item from the store must still be active
-- ---------------------------------------------------------------------------
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
  if coalesce(trim(p_recipient_name), '') = '' or coalesce(trim(p_city), '') = ''
     or coalesce(trim(p_address_line), '') = '' then
    raise exception 'recipient name, city and address are required';
  end if;
  if p_recipient_phone is null or p_recipient_phone !~ '^\+?[0-9 ]{7,20}$' then
    raise exception 'recipient phone is not valid';
  end if;

  -- lock the listings so two buyers can't take the last item at the same time
  for v_item in
    select l.id, l.title, l.status, l.price_minor, l.currency, l.stock_quantity, ci.quantity
    from public.cart_items ci
    join public.listings l on l.id = ci.listing_id
    where ci.user_id = v_buyer and l.store_id = p_store_id
    for update of l
  loop
    if v_item.status <> 'active' then
      raise exception '"%" is no longer available', v_item.title;
    end if;
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
          trim(p_recipient_name), p_recipient_phone, p_country_code, trim(p_city), trim(p_address_line), p_note)
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

-- ---------------------------------------------------------------------------
-- 4. Order status: forward only, and cancelling restocks
-- ---------------------------------------------------------------------------
create or replace function public.check_order_status()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.status = old.status then
    return new;
  end if;
  if not (
    (old.status = 'pending'   and new.status in ('confirmed', 'cancelled')) or
    (old.status = 'confirmed' and new.status in ('shipped', 'cancelled')) or
    (old.status = 'shipped'   and new.status = 'delivered')
  ) then
    raise exception 'order status cannot change from % to %', old.status, new.status;
  end if;

  if new.status = 'cancelled' then
    update public.listings l
    set stock_quantity = l.stock_quantity + oi.quantity,
        status = case when l.status = 'sold' then 'active'::public.listing_status else l.status end
    from public.order_items oi
    where oi.order_id = new.id and oi.listing_id = l.id and l.stock_quantity is not null;
  end if;
  return new;
end $$;
create trigger orders_status_check before update of status on public.orders
  for each row execute function public.check_order_status();

-- Buyers cancel their own order while the store has not confirmed it yet.
create or replace function public.cancel_order(p_order_id uuid)
returns void
language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  update public.orders set status = 'cancelled'
  where id = p_order_id and buyer_id = auth.uid() and status = 'pending';
  if not found then
    raise exception 'order not found or can no longer be cancelled';
  end if;
end $$;
revoke execute on function public.cancel_order from public, anon;
grant  execute on function public.cancel_order to authenticated;

-- ---------------------------------------------------------------------------
-- 5. Reviews
-- ---------------------------------------------------------------------------
-- NULL listing_id used to count as "different", so the same review could be posted
-- again and again. Keep the newest of any duplicates, then make NULLs count as equal.
delete from public.reviews r
using public.reviews newer
where r.reviewer_id = newer.reviewer_id and r.seller_id = newer.seller_id
  and r.listing_id is not distinct from newer.listing_id
  and (r.created_at, r.id) < (newer.created_at, newer.id);
alter table public.reviews drop constraint reviews_reviewer_id_seller_id_listing_id_key;
alter table public.reviews add constraint reviews_one_per_listing
  unique nulls not distinct (reviewer_id, seller_id, listing_id);

-- Only people who chatted with or ordered from the seller can review them.
create or replace function public.check_review_allowed()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if not exists (
       select 1 from public.conversations c
       where c.buyer_id = new.reviewer_id and c.seller_id = new.seller_id
     ) and not exists (
       select 1 from public.orders o join public.stores s on s.id = o.store_id
       where o.buyer_id = new.reviewer_id and s.owner_id = new.seller_id and o.status <> 'cancelled'
     ) then
    raise exception 'you can only review a seller you have chatted with or bought from';
  end if;
  return new;
end $$;
create trigger reviews_allowed before insert on public.reviews
  for each row execute function public.check_review_allowed();

revoke update on public.reviews from anon, authenticated;
grant update (rating, comment) on public.reviews to authenticated;

-- ---------------------------------------------------------------------------
-- 6. Profiles: public card only; phone and settings via get_my_profile()
-- ---------------------------------------------------------------------------
revoke select on public.profiles from anon, authenticated;
grant select (id, full_name, avatar_url, country_code, city, is_verified, created_at)
  on public.profiles to anon, authenticated;

create or replace function public.get_my_profile()
returns public.profiles
language sql stable security definer set search_path = '' as $$
  select * from public.profiles where id = auth.uid();
$$;
revoke execute on function public.get_my_profile from public, anon;
grant  execute on function public.get_my_profile to authenticated;

-- ---------------------------------------------------------------------------
-- 7. Listing images must live in the seller's own storage folder
-- ---------------------------------------------------------------------------
drop policy "sellers add images" on public.listing_images;
create policy "sellers add images" on public.listing_images for insert
  with check (storage_path like auth.uid()::text || '/%'
              and exists (select 1 from public.listings l where l.id = listing_id and l.seller_id = auth.uid()));
drop policy "sellers change images" on public.listing_images;
create policy "sellers change images" on public.listing_images for update
  using (exists (select 1 from public.listings l where l.id = listing_id and l.seller_id = auth.uid()))
  with check (storage_path like auth.uid()::text || '/%'
              and exists (select 1 from public.listings l where l.id = listing_id and l.seller_id = auth.uid()));

-- ---------------------------------------------------------------------------
-- 8. search_listings: clamp paging, treat % and _ literally
-- ---------------------------------------------------------------------------
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
    and (p_city    is null or lower(l.city) = lower(trim(p_city)))
    and (p_type    is null or l.listing_type = p_type)
    and (coalesce(trim(q), '') = ''
         or l.search @@ websearch_to_tsquery('simple', q)
         or l.title ilike '%' || replace(replace(replace(trim(q), '\', '\\'), '%', '\%'), '_', '\_') || '%')
  order by
    case when coalesce(trim(q), '') = '' then 0
         else ts_rank(l.search, websearch_to_tsquery('simple', q)) end desc,
    l.created_at desc
  limit least(greatest(coalesce(p_limit, 30), 1), 100) offset greatest(coalesce(p_offset, 0), 0)
$$;

-- ---------------------------------------------------------------------------
-- 9. Live chat: publish new messages and conversation updates to the app
-- ---------------------------------------------------------------------------
do $$
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    alter publication supabase_realtime add table public.messages, public.conversations;
  end if;
end $$;
