-- =============================================================================
-- እንካን ሃባን — ናይ ፈተነ ዳታ / DEMO DATA (for trying the app, NOT a migration)
--
-- How to use: Supabase → SQL Editor → paste this whole file → Run.
-- Safe to run more than once (fixed ids + "on conflict do nothing").
-- Remove everything again with seed/remove_demo_data.sql.
--
-- What it adds
--   * 2 demo sellers (auth users with no password — nobody can sign in as them)
--   * 1 shop owned by the second seller
--   * 12 active listings in UGX across several categories (classified ads + shop items)
-- No photos: the app shows the category emoji instead.
-- =============================================================================
begin;

-- ---------------------------------------------------------------------------
-- 1. Demo sellers. The on_auth_user_created trigger creates their profiles.
-- ---------------------------------------------------------------------------
insert into auth.users (
  id, instance_id, aud, role, email, encrypted_password, email_confirmed_at,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
  confirmation_token, recovery_token, email_change_token_new, email_change
) values
  ('d0000000-0000-4000-8000-000000000001', '00000000-0000-0000-0000-000000000000',
   'authenticated', 'authenticated', 'demo.seller1@example.com', '', now(),
   '{"provider":"email","providers":["email"]}', '{"full_name":"ሳባ ተስፋይ"}', now(), now(),
   '', '', '', ''),
  ('d0000000-0000-4000-8000-000000000002', '00000000-0000-0000-0000-000000000000',
   'authenticated', 'authenticated', 'demo.seller2@example.com', '', now(),
   '{"provider":"email","providers":["email"]}', '{"full_name":"ዳዊት ገብረሂወት"}', now(), now(),
   '', '', '', '')
on conflict (id) do nothing;

update public.profiles set phone = '+256 700 111 222', country_code = 'UG', city = 'Kampala', is_verified = true
  where id = 'd0000000-0000-4000-8000-000000000001';
update public.profiles set phone = '+256 770 333 444', country_code = 'UG', city = 'Kampala'
  where id = 'd0000000-0000-4000-8000-000000000002';

-- ---------------------------------------------------------------------------
-- 2. A shop for the second seller (store listings must belong to a shop)
-- ---------------------------------------------------------------------------
insert into public.stores (id, owner_id, name, slug, description, phone, country_code, city, is_verified)
values ('d0000000-0000-4000-8000-0000000000a1', 'd0000000-0000-4000-8000-000000000002',
        'ዱኳን ሓበሻ', 'demo-dukan-habesha', 'ባህላዊ ክዳውንትን ቀመማትን ካብ ኤርትራን ኢትዮጵያን',
        '+256 770 333 444', 'UG', 'Kampala', true)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- 3. Listings (prices are whole Ugandan shillings: UGX has no minor unit)
--    created_at is staggered so the feed shows a clear newest-first order.
-- ---------------------------------------------------------------------------
insert into public.listings (
  id, seller_id, store_id, category_id, listing_type, status, title, description, condition,
  price_minor, old_price_minor, currency, is_negotiable, stock_quantity,
  country_code, city, area, contact_phone, created_at
) values
  -- classified ads, seller 1
  ('d0000000-0000-4000-8000-000000000101', 'd0000000-0000-4000-8000-000000000001', null,
   'phones-smartphones', 'classified', 'active', 'Samsung Galaxy A54 128GB',
   'ጽቡቕ ኩነታት፣ ምስ ቻርጀርን ሽፋንን። 8 ወርሒ ጥራይ ተጠቒመሉ።', 'used',
   950000, null, 'UGX', true, null, 'UG', 'Kampala', 'Kabalagala', '+256 700 111 222', now() - interval '1 hour'),
  ('d0000000-0000-4000-8000-000000000102', 'd0000000-0000-4000-8000-000000000001', null,
   'phones-smartphones', 'classified', 'active', 'iPhone 12 64GB',
   'ባትሪ 86%፣ ስክሪን ጽቡቕ። ዋጋ ይዘራረብ።', 'used',
   1400000, null, 'UGX', true, null, 'UG', 'Kampala', 'Nsambya', '+256 700 111 222', now() - interval '3 hours'),
  ('d0000000-0000-4000-8000-000000000103', 'd0000000-0000-4000-8000-000000000001', null,
   'electronics', 'classified', 'active', 'HP EliteBook 840 laptop',
   'Core i5, 8GB RAM, 256GB SSD። ንትምህርትን ስራሕን ዝኸውን።', 'refurbished',
   1250000, null, 'UGX', false, null, 'UG', 'Kampala', 'Wandegeya', '+256 700 111 222', now() - interval '6 hours'),
  ('d0000000-0000-4000-8000-000000000104', 'd0000000-0000-4000-8000-000000000001', null,
   'home', 'classified', 'active', 'ሶፋ 3 ሰብ / 3-seater sofa',
   'ሕብሪ ግራጫ፣ ጽሩይ። ካብ ገዛ ክውሰድ ኣለዎ።', 'used',
   800000, null, 'UGX', true, null, 'UG', 'Kampala', 'Muyenga', '+256 700 111 222', now() - interval '1 day'),
  ('d0000000-0000-4000-8000-000000000105', 'd0000000-0000-4000-8000-000000000001', null,
   'property-rent', 'classified', 'active', 'ክፍሊ ንኽራይ ኣብ ካባላጋላ',
   'ሓደ መደቀሲ ክፍልን ሳሎንን፣ ማይን መብራህትን ዘለዎ። ወርሓዊ ክፍሊት።', 'used',
   600000, null, 'UGX', true, null, 'UG', 'Kampala', 'Kabalagala', '+256 700 111 222', now() - interval '2 days'),
  ('d0000000-0000-4000-8000-000000000106', 'd0000000-0000-4000-8000-000000000001', null,
   'vehicles', 'classified', 'active', 'Toyota Premio 2008',
   'ሓድሽ ጎማታት፣ ብጽቡቕ ይጎዓዝ። ወረቐት ምሉእ።', 'used',
   28000000, null, 'UGX', true, null, 'UG', 'Kampala', 'Ntinda', '+256 700 111 222', now() - interval '3 days'),
  ('d0000000-0000-4000-8000-000000000107', 'd0000000-0000-4000-8000-000000000001', null,
   'services', 'classified', 'active', 'ትምህርቲ ትግርኛ ንቆልዑ',
   'ቀዳመ-ሰንበት ኣብ ገዛኹም። ንሓደ ሰዓት ዋጋ።', 'new',
   30000, null, 'UGX', false, null, 'UG', 'Kampala', null, '+256 700 111 222', now() - interval '4 days'),

  -- shop items, seller 2 (bought through the cart)
  ('d0000000-0000-4000-8000-000000000201', 'd0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-0000000000a1',
   'habesha-wear-women', 'store', 'active', 'ዘሪያ ቀሚሽ ሓበሻ ብጥልፊ',
   'ብኢድ ዝተሰርሐ ጥልፊ፣ ጥጡ። መጠናት S–XL።', 'new',
   450000, 520000, 'UGX', false, 6, 'UG', 'Kampala', 'Kansanga', null, now() - interval '2 hours'),
  ('d0000000-0000-4000-8000-000000000202', 'd0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-0000000000a1',
   'habesha-wear-men', 'store', 'active', 'ጋቢ ደቂ ተባዕትዮ',
   'ሙቐት ዘለዎ ጋቢ ጥጡ።', 'new',
   180000, null, 'UGX', false, 10, 'UG', 'Kampala', 'Kansanga', null, now() - interval '5 hours'),
  ('d0000000-0000-4000-8000-000000000203', 'd0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-0000000000a1',
   'habesha-food', 'store', 'active', 'በርበረ 1 ኪሎ',
   'ካብ ዓዲ ዝመጸ ንጹህ በርበረ።', 'new',
   45000, 55000, 'UGX', false, 40, 'UG', 'Kampala', 'Kansanga', null, now() - interval '12 hours'),
  ('d0000000-0000-4000-8000-000000000204', 'd0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-0000000000a1',
   'habesha-food', 'store', 'active', 'ቡን ሓበሻ 500 ግራም',
   'ዝተቖልወ ቡን፣ ንጀበና ዝኸውን።', 'new',
   35000, null, 'UGX', false, 25, 'UG', 'Kampala', 'Kansanga', null, now() - interval '2 days'),
  ('d0000000-0000-4000-8000-000000000205', 'd0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-0000000000a1',
   'books', 'store', 'active', 'መጽሓፍ ቅዱስ ብትግርኛ',
   'ዓቢ ፊደል፣ ብቆርበት ዝተሸፈነ።', 'new',
   70000, null, 'UGX', false, 12, 'UG', 'Kampala', 'Kansanga', null, now() - interval '5 days')
on conflict (id) do nothing;

commit;

-- Check: should list the 12 demo items, newest first
select title, listing_type, price_minor, currency, city
from public.search_listings(p_limit => 50)
where seller_id in ('d0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000002');
