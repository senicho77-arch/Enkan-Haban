-- Removes everything seed/demo_data.sql added.
-- Deleting the two demo auth users cascades to their profiles, shop, listings, images,
-- favorites, cart items and chats. Real users are not touched.
delete from auth.users
where id in ('d0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000002');
