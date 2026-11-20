-- =============================================================================
-- ስእሊ ማስታወቂያ / Advert banners on the home screen — posted by admins only
--   * public.promo_banners: everyone reads the active banners inside their dates;
--     only admins (public.is_admin()) create, change or delete them.
--   * Banner pictures live in the listing-images bucket under promos/ — only admins
--     may upload, change or delete there; everyone may view (bucket is public).
-- =============================================================================

create table public.promo_banners (
  id                 uuid primary key default gen_random_uuid(),
  image_path         text check (image_path is null or image_path like 'promos/%'),
  tag                text not null default '' check (char_length(tag) <= 30),
  title              text not null default '' check (char_length(title) <= 60),
  subtitle           text not null default '' check (char_length(subtitle) <= 120),
  cta                text not null default '' check (char_length(cta) <= 24),
  emoji              text not null default '' check (char_length(emoji) <= 16),
  color_start        text not null default '#3FA9E8' check (color_start ~ '^#[0-9A-Fa-f]{6}$'),
  color_end          text not null default '#1B5E8C' check (color_end ~ '^#[0-9A-Fa-f]{6}$'),
  target_category_id text references public.categories (id) on delete set null,
  target_listing_id  uuid references public.listings (id) on delete set null,
  sort_order         integer not null default 0,
  is_active          boolean not null default true,
  starts_at          timestamptz,
  ends_at            timestamptz,
  created_by         uuid references public.profiles (id) on delete set null default auth.uid(),
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now(),
  constraint promo_banners_has_content check (image_path is not null or title <> ''),
  constraint promo_banners_one_target check (target_category_id is null or target_listing_id is null),
  constraint promo_banners_dates check (starts_at is null or ends_at is null or ends_at > starts_at)
);
create index promo_banners_order_idx on public.promo_banners (sort_order, created_at);
create trigger promo_banners_updated_at before update on public.promo_banners
  for each row execute function public.set_updated_at();

alter table public.promo_banners enable row level security;

create policy "everyone reads live banners" on public.promo_banners for select to anon, authenticated
  using (is_active and (starts_at is null or starts_at <= now()) and (ends_at is null or ends_at > now()));
create policy "admins read all banners" on public.promo_banners for select to authenticated
  using (public.is_admin());
create policy "admins add banners" on public.promo_banners for insert to authenticated
  with check (public.is_admin());
create policy "admins change banners" on public.promo_banners for update to authenticated
  using (public.is_admin()) with check (public.is_admin());
create policy "admins delete banners" on public.promo_banners for delete to authenticated
  using (public.is_admin());

revoke all on public.promo_banners from anon, authenticated;
grant select on public.promo_banners to anon, authenticated;
grant insert, update, delete on public.promo_banners to authenticated;  -- RLS: admins only

-- ---------------------------------------------------------------------------
-- Banner pictures: listing-images/promos/<banner id>.jpg, admins only
-- ---------------------------------------------------------------------------
create policy "admins upload promo images" on storage.objects for insert to authenticated
  with check (bucket_id = 'listing-images' and (storage.foldername(name))[1] = 'promos' and public.is_admin());
create policy "admins update promo images" on storage.objects for update to authenticated
  using (bucket_id = 'listing-images' and (storage.foldername(name))[1] = 'promos' and public.is_admin());
create policy "admins delete promo images" on storage.objects for delete to authenticated
  using (bucket_id = 'listing-images' and (storage.foldername(name))[1] = 'promos' and public.is_admin());
