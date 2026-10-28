-- =============================================================================
-- ስእልታት / Image storage
-- Public bucket for listing photos, avatars and store logos.
-- Each user uploads only into a folder named after their own user id:
--   listing-images/<user id>/<listing id>/1.jpg
-- =============================================================================
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('listing-images', 'listing-images', true, 5242880,          -- 5 MB per photo
        array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do nothing;

create policy "anyone views listing images" on storage.objects for select
  using (bucket_id = 'listing-images');

create policy "users upload to own folder" on storage.objects for insert to authenticated
  with check (bucket_id = 'listing-images' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "users update own images" on storage.objects for update to authenticated
  using (bucket_id = 'listing-images' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "users delete own images" on storage.objects for delete to authenticated
  using (bucket_id = 'listing-images' and (storage.foldername(name))[1] = auth.uid()::text);
