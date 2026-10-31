-- =============================================================================
-- ደውለለይ / Request call back (Jiji-style)
-- A buyer leaves their name and number on a classified ad; the seller sees the
-- request and calls back. Buyers create requests only through request_callback().
-- =============================================================================

create type public.callback_status as enum ('new', 'done');

create table public.callback_requests (
  id          uuid primary key default gen_random_uuid(),
  listing_id  uuid not null references public.listings (id) on delete cascade,
  buyer_id    uuid not null references public.profiles (id) on delete cascade,
  seller_id   uuid not null references public.profiles (id) on delete cascade,
  buyer_name  text not null check (char_length(buyer_name) between 1 and 80),
  buyer_phone text not null check (buyer_phone ~ '^\+?[0-9 ]{7,20}$'),
  status      public.callback_status not null default 'new',
  created_at  timestamptz not null default now(),
  check (buyer_id <> seller_id)
);
create index callback_requests_seller_idx on public.callback_requests (seller_id, status, created_at desc);
create index callback_requests_buyer_idx  on public.callback_requests (buyer_id, created_at desc);
-- one open request per buyer per ad (asking again just updates it)
create unique index callback_requests_open_uniq on public.callback_requests (listing_id, buyer_id) where status = 'new';

alter table public.callback_requests enable row level security;
create policy "buyer and seller see callback" on public.callback_requests for select
  using (buyer_id = auth.uid() or seller_id = auth.uid());
create policy "seller marks callback done" on public.callback_requests for update
  using (seller_id = auth.uid()) with check (seller_id = auth.uid());

revoke insert, update, delete on public.callback_requests from anon, authenticated;
revoke select on public.callback_requests from anon;
grant update (status) on public.callback_requests to authenticated;

create or replace function public.request_callback(p_listing_id uuid, p_name text, p_phone text)
returns uuid
language plpgsql security definer set search_path = '' as $$
declare
  v_buyer  uuid := auth.uid();
  v_seller uuid;
  v_id     uuid;
begin
  if v_buyer is null then raise exception 'not signed in'; end if;
  select seller_id into v_seller from public.listings
   where id = p_listing_id and status = 'active' and listing_type = 'classified';
  if v_seller is null then raise exception 'listing not found'; end if;
  if v_seller = v_buyer then raise exception 'you cannot request a call from yourself'; end if;
  if coalesce(trim(p_name), '') = '' then raise exception 'name is required'; end if;
  if p_phone is null or trim(p_phone) !~ '^\+?[0-9 ]{7,20}$' then raise exception 'phone is not valid'; end if;

  update public.callback_requests
     set buyer_name = trim(p_name), buyer_phone = trim(p_phone), created_at = now()
   where listing_id = p_listing_id and buyer_id = v_buyer and status = 'new'
  returning id into v_id;
  if v_id is null then
    insert into public.callback_requests (listing_id, buyer_id, seller_id, buyer_name, buyer_phone)
    values (p_listing_id, v_buyer, v_seller, trim(p_name), trim(p_phone))
    returning id into v_id;
  end if;
  return v_id;
end $$;

revoke execute on function public.request_callback from public, anon;
grant  execute on function public.request_callback to authenticated;
