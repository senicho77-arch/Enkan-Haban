-- =============================================================================
-- ኣድሚን ዝውስኖ መስርዕ ፖስትታት ኣብ ነፍሲ ወከፍ ምድብ / Admin-controlled listing order per category
--   * public.admins            — who is an app admin (added by the owner in the SQL editor).
--   * public.category_placements — hand-made order of listings inside one category
--                                ('all' = home feed with no category selected).
--   * admin_* RPCs             — the only way to change placements (admins only).
--   * search_listings          — same filters; placed listings first (in the admin's order),
--                                then the rest newest first. With a search text, relevance wins
--                                and the admin order is only a tiebreaker.
-- Placements of listings that are no longer active, no longer in that category, or whose
-- pinned_until has passed are simply ignored — no cron job needed.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Admins
-- ---------------------------------------------------------------------------
create table public.admins (
  user_id    uuid primary key references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now()
);
alter table public.admins enable row level security;
-- no policies and no grants for anon/authenticated → nobody can read or write it from the app
revoke all on public.admins from anon, authenticated;

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.admins a where a.user_id = auth.uid())
$$;
revoke execute on function public.is_admin() from public, anon;
grant  execute on function public.is_admin() to authenticated;

-- ---------------------------------------------------------------------------
-- 2. Placements
-- ---------------------------------------------------------------------------
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
create index category_placements_listing_idx on public.category_placements (listing_id);

alter table public.category_placements enable row level security;
create policy "placements are public" on public.category_placements for select using (true);
-- writes only through the admin RPCs below
revoke all on public.category_placements from anon, authenticated;
grant select on public.category_placements to anon, authenticated;

-- ---------------------------------------------------------------------------
-- 3. Helpers (internal, not callable from the app)
-- ---------------------------------------------------------------------------
create or replace function public.check_placement_category(p_category_id text)
returns void language plpgsql stable security definer set search_path = '' as $$
begin
  if p_category_id is null or (p_category_id <> 'all'
     and not exists (select 1 from public.categories c where c.id = p_category_id)) then
    raise exception 'unknown category "%"', p_category_id;
  end if;
end $$;

-- true when the listing is active and belongs to the category (or one of its sub-categories)
create or replace function public.listing_fits_category(p_listing_id uuid, p_category_id text)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.listings l
    where l.id = p_listing_id and l.status = 'active'
      and (p_category_id = 'all' or l.category_id = p_category_id
           or l.category_id in (select c.id from public.categories c where c.parent_id = p_category_id))
  )
$$;

-- renumber a category's placements to 1..n without gaps (keeps the current order)
create or replace function public.renumber_placements(p_category_id text)
returns void language sql security definer set search_path = '' as $$
  update public.category_placements p
  set position = r.rn, updated_at = now()
  from (select listing_id, row_number() over (order by position, updated_at, listing_id) as rn
        from public.category_placements where category_id = p_category_id) r
  where p.category_id = p_category_id and p.listing_id = r.listing_id and p.position <> r.rn
$$;

revoke execute on function public.check_placement_category(text) from public, anon, authenticated;
revoke execute on function public.listing_fits_category(uuid, text) from public, anon, authenticated;
revoke execute on function public.renumber_placements(text) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- 4. Admin RPCs
-- ---------------------------------------------------------------------------
-- Replace the whole order of one category. Empty array = clear it.
create or replace function public.admin_set_category_order(p_category_id text, p_listing_ids uuid[])
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_ids uuid[] := coalesce(p_listing_ids, '{}');
  v_bad uuid;
  v_old jsonb;
begin
  if not public.is_admin() then raise exception 'only admins can change the listing order'; end if;
  perform public.check_placement_category(p_category_id);

  if array_position(v_ids, null) is not null then
    raise exception 'listing ids must not be null';
  end if;
  if (select count(distinct x) from unnest(v_ids) x) <> cardinality(v_ids) then
    raise exception 'duplicate listing in order';
  end if;
  select x into v_bad from unnest(v_ids) x
  where not public.listing_fits_category(x, p_category_id) limit 1;
  if v_bad is not null then
    raise exception 'listing % is not an active listing in category "%"', v_bad, p_category_id;
  end if;

  perform pg_advisory_xact_lock(hashtext('category_placements:' || p_category_id));

  -- keep pinned_until of listings that were already placed
  select coalesce(jsonb_object_agg(listing_id::text, pinned_until), '{}') into v_old
  from public.category_placements where category_id = p_category_id;

  delete from public.category_placements where category_id = p_category_id;

  insert into public.category_placements (category_id, listing_id, position, pinned_until, created_by)
  select p_category_id, u.id, u.ord::int, (v_old ->> u.id::text)::timestamptz, auth.uid()
  from unnest(v_ids) with ordinality as u (id, ord);
end $$;

-- Insert or move one listing to p_position (others shift down); positions stay 1..n.
create or replace function public.admin_pin_listing(
  p_category_id  text,
  p_listing_id   uuid,
  p_position     int default 1,
  p_pinned_until timestamptz default null
) returns void language plpgsql security definer set search_path = '' as $$
declare
  v_count int;
  v_pos   int;
begin
  if not public.is_admin() then raise exception 'only admins can change the listing order'; end if;
  perform public.check_placement_category(p_category_id);
  if not public.listing_fits_category(p_listing_id, p_category_id) then
    raise exception 'listing % is not an active listing in category "%"', p_listing_id, p_category_id;
  end if;

  -- serialise changes to one category
  perform pg_advisory_xact_lock(hashtext('category_placements:' || p_category_id));

  delete from public.category_placements where category_id = p_category_id and listing_id = p_listing_id;
  perform public.renumber_placements(p_category_id);

  select count(*) into v_count from public.category_placements where category_id = p_category_id;
  v_pos := least(greatest(coalesce(p_position, 1), 1), v_count + 1);

  update public.category_placements set position = position + 1, updated_at = now()
  where category_id = p_category_id and position >= v_pos;

  insert into public.category_placements (category_id, listing_id, position, pinned_until, created_by)
  values (p_category_id, p_listing_id, v_pos, p_pinned_until, auth.uid());
end $$;

-- Remove one listing from the order and close the gap.
create or replace function public.admin_unpin_listing(p_category_id text, p_listing_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'only admins can change the listing order'; end if;
  perform public.check_placement_category(p_category_id);
  perform pg_advisory_xact_lock(hashtext('category_placements:' || p_category_id));
  delete from public.category_placements where category_id = p_category_id and listing_id = p_listing_id;
  perform public.renumber_placements(p_category_id);
end $$;

-- The placed listings of one category, in order — for the admin screen.
create or replace function public.admin_get_category_order(p_category_id text)
returns table (listing_id uuid, title text, "position" int, pinned_until timestamptz, is_expired boolean)
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'only admins can see the listing order'; end if;
  perform public.check_placement_category(p_category_id);
  return query
    select p.listing_id, l.title, p.position, p.pinned_until,
           (p.pinned_until is not null and p.pinned_until <= now())
    from public.category_placements p
    join public.listings l on l.id = p.listing_id
    where p.category_id = p_category_id
    order by p.position, p.listing_id;
end $$;

revoke execute on function public.admin_set_category_order(text, uuid[]) from public, anon;
revoke execute on function public.admin_pin_listing(text, uuid, int, timestamptz) from public, anon;
revoke execute on function public.admin_unpin_listing(text, uuid) from public, anon;
revoke execute on function public.admin_get_category_order(text) from public, anon;
grant  execute on function public.admin_set_category_order(text, uuid[]) to authenticated;
grant  execute on function public.admin_pin_listing(text, uuid, int, timestamptz) to authenticated;
grant  execute on function public.admin_unpin_listing(text, uuid) to authenticated;
grant  execute on function public.admin_get_category_order(text) to authenticated;

-- ---------------------------------------------------------------------------
-- 5. search_listings: same signature and filters, admin order first when browsing
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
  left join public.category_placements p
    on p.listing_id = l.id
   and p.category_id = coalesce(p_category_id, 'all')
   and (p.pinned_until is null or p.pinned_until > now())
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
    (p.position is null),      -- placed first
    p.position asc,
    l.created_at desc,
    l.id                       -- stable paging
  limit least(greatest(coalesce(p_limit, 30), 1), 100) offset greatest(coalesce(p_offset, 0), 0)
$$;
