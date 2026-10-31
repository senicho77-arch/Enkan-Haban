-- Security and behaviour tests for the migrations. Run with supabase/tests/run.sh
-- Acts as real app users (anon / authenticated) and prints PASS/FAIL per check.
\set ON_ERROR_STOP 1
create schema t; grant usage on schema t to anon, authenticated;
create table t.results (n serial, name text, pass boolean, detail text); grant all on t.results, t.results_n_seq to anon, authenticated;
create function t.ok(cond boolean, name text) returns void language sql as $$ insert into t.results (name, pass) values (name, coalesce(cond, false)) $$;
create function t.fails(stmt text, pattern text, name text) returns void language plpgsql as $$
begin
  begin execute stmt; exception when others then
    insert into t.results (name, pass, detail) values (name, sqlerrm ilike '%' || pattern || '%', sqlerrm); return;
  end;
  insert into t.results (name, pass, detail) values (name, false, 'did not fail');
end $$;
create function t.act(u text) returns void language sql as $$
  select set_config('request.jwt.claim.sub', u, false), set_config('request.jwt.claim.role', 'authenticated', false); $$;

insert into auth.users (id, raw_user_meta_data) values
 ('00000000-0000-0000-0000-00000000000a', '{"full_name":"Seller A"}'),
 ('00000000-0000-0000-0000-00000000000b', '{"name":"Buyer B", "picture":"http://x/b.png"}'),
 ('00000000-0000-0000-0000-00000000000c', '{}');
select t.ok((select full_name from profiles where id = '00000000-0000-0000-0000-00000000000b') = 'Buyer B', 'profile auto-created from Google metadata');

-- Seller A: store + listings
select t.act('00000000-0000-0000-0000-00000000000a'); set role authenticated;
update profiles set phone = '+256 700 111 222' where id = auth.uid();
insert into stores (owner_id, name, slug, country_code, city) values (auth.uid(), 'Shop A', 'shop-a', 'UG', 'Kampala') returning id as store \gset
select t.fails($$insert into stores (owner_id, name, slug, country_code, city, is_verified) values (auth.uid(), 'S', 'sss', 'UG', 'K', true)$$, 'permission denied', 'cannot self-verify a store');
insert into listings (seller_id, store_id, category_id, listing_type, title, price_minor, currency, stock_quantity, country_code, city)
  values (auth.uid(), :'store', 'phones-smartphones', 'store', 'Tecno Spark 20', 45000, 'UGX', 2, 'UG', 'Kampala') returning id as phone \gset
insert into listings (seller_id, store_id, category_id, listing_type, title, price_minor, currency, stock_quantity, country_code, city)
  values (auth.uid(), :'store', 'phones-accessories', 'store', 'Charger 100%_fast', 5000, 'UGX', 10, 'UG', 'Kampala') returning id as charger \gset
insert into listings (seller_id, category_id, listing_type, title, price_minor, currency, country_code, city, contact_phone)
  values (auth.uid(), 'habesha-wear-women', 'classified', 'ዘሪያ ጥበብ ቀሚሽ', 650000, 'ETB', 'ET', 'Addis Ababa', '+251 911 000 000') returning id as kemis \gset
select t.fails($$insert into listings (seller_id, category_id, listing_type, title, price_minor, currency, country_code, city, view_count) values (auth.uid(), 'phones', 'classified', 'Fake views', 1, 'USD', 'UG', 'Kampala', 999999)$$, 'permission denied', 'cannot fake view_count on insert');
select t.fails($$insert into listings (seller_id, category_id, listing_type, title, price_minor, currency, country_code, city, status) values (auth.uid(), 'phones', 'classified', 'Removed', 1, 'USD', 'UG', 'Kampala', 'removed')$$, 'moderators', 'cannot insert as removed');
select t.fails($$insert into listings (seller_id, category_id, listing_type, title, price_minor, currency, stock_quantity, country_code, city, store_id) values ('00000000-0000-0000-0000-00000000000b', 'phones', 'store', 'Not mine', 1, 'USD', 1, 'UG', 'Kampala', null)$$, 'row-level security', 'cannot post as someone else');

-- Search (as anon)
reset role; set role anon;
select t.ok((select count(*) from search_listings(p_category_id => 'phones')) = 2, 'category search includes sub-categories');
select t.ok((select count(*) from search_listings(q => 'ዘሪያ')) = 1, 'Tigrinya full-text search');
select t.ok((select count(*) from search_listings(q => 'tecno')) = 1, 'case-insensitive search');
select t.ok((select count(*) from search_listings(q => '100%_')) = 1, 'literal % and _ in search text');
select t.ok((select count(*) from search_listings(q => 'n%k')) = 0, '% is not a wildcard (n%k does not match Tecno Spark)');
select t.ok((select count(*) from search_listings(p_limit => -5, p_offset => -1)) = 1, 'negative limit/offset are clamped');
select t.ok((select count(*) from search_listings(p_city => 'kampala')) = 2, 'city filter ignores case');
select t.fails($$select phone from profiles$$, 'permission denied', 'anon cannot read phone numbers');
select t.ok((select count(*) from profiles) = 3, 'anon can read public profile cards');
select t.fails($$select place_order(gen_random_uuid(), 'x', '+256700000000', 'UG', 'K', 'a')$$, 'permission denied', 'anon cannot place orders');

-- Buyer B: profile privacy, cart, order
reset role; select t.act('00000000-0000-0000-0000-00000000000b'); set role authenticated;
select t.ok((select phone from get_my_profile()) is null and (select full_name from get_my_profile()) = 'Buyer B', 'get_my_profile returns own row');
select t.fails($$select phone from profiles$$, 'permission denied', 'users cannot read others'' phones');
select t.fails($$update profiles set is_verified = true where id = auth.uid()$$, 'permission denied', 'cannot self-verify profile');
select t.fails(format($$insert into cart_items values (auth.uid(), %L, 1)$$, :'kemis'), 'only active store listings', 'classified ads cannot go in cart');
insert into cart_items values (auth.uid(), :'phone', 2), (auth.uid(), :'charger', 1);
select t.fails(format($$select place_order(%L, ' ', '+256700000000', 'UG', 'Kampala', 'Plot 1')$$, :'store'), 'required', 'place_order requires recipient name');
select t.fails(format($$select place_order(%L, 'B', 'call me', 'UG', 'Kampala', 'Plot 1')$$, :'store'), 'phone', 'place_order validates phone');
-- seller pauses the charger after it was added to the cart
reset role; update listings set status = 'paused' where id = :'charger'; set role authenticated;
select t.fails(format($$select place_order(%L, 'B', '+256700000000', 'UG', 'Kampala', 'Plot 1')$$, :'store'), 'no longer available', 'paused item cannot be ordered');
delete from cart_items where listing_id = :'charger';
select place_order(:'store', 'B', '+256 700 000 000', 'UG', 'Kampala', 'Plot 1') as order1 \gset
select t.ok((select subtotal_minor = 90000 and total_minor = 90000 from orders where id = :'order1'), 'order total read from database');
select t.ok((select count(*) from cart_items) = 0, 'cart emptied after order');
reset role;
select t.ok((select stock_quantity = 0 and status = 'sold' from listings where id = :'phone'), 'stock reaches 0 -> sold');
set role authenticated;
update orders set status = 'delivered' where id = :'order1';
select t.ok((select status from orders where id = :'order1') = 'pending', 'buyer cannot change order status');
select cancel_order(:'order1');
reset role;
select t.ok((select status from orders where id = :'order1') = 'cancelled', 'buyer can cancel pending order');
select t.ok((select stock_quantity = 2 and status = 'active' from listings where id = :'phone'), 'cancel restores stock and reactivates');
set role authenticated;
select t.fails(format($$select cancel_order(%L)$$, :'order1'), 'can no longer be cancelled', 'cannot cancel twice');

-- Second order; store owner moves it forward only
insert into cart_items values (auth.uid(), :'phone', 1);
select place_order(:'store', 'B', '+256700000000', 'UG', 'Kampala', 'Plot 1') as order2 \gset
reset role; select t.act('00000000-0000-0000-0000-00000000000a'); set role authenticated;
update orders set status = 'confirmed' where id = :'order2';
update orders set status = 'shipped' where id = :'order2';
select t.fails(format($$update orders set status = 'pending' where id = %L$$, :'order2'), 'cannot change', 'order status cannot go backwards');
select t.fails(format($$update orders set status = 'cancelled' where id = %L$$, :'order2'), 'cannot change', 'shipped order cannot be cancelled');
update orders set status = 'delivered' where id = :'order2';
select t.fails(format($$update orders set subtotal_minor = 1 where id = %L$$, :'order2'), 'permission denied', 'store owner cannot change order totals');

-- Moderation
reset role; update listings set status = 'removed' where id = :'kemis'; set role authenticated;
select t.fails(format($$update listings set status = 'active' where id = %L$$, :'kemis'), 'moderators', 'seller cannot undo a removal');
select t.fails(format($$update listings set status = 'removed' where id = %L$$, :'phone'), 'moderators', 'seller cannot set removed');
update listings set status = 'paused' where id = :'phone';
update listings set status = 'active' where id = :'phone';
select t.ok(true, 'seller can pause and reactivate');
reset role; update listings set status = 'active' where id = :'kemis';
select t.ok(true, 'admin can restore a removed listing');

-- Chat
select t.act('00000000-0000-0000-0000-00000000000b'); set role authenticated;
select start_conversation(:'kemis') as conv \gset
select t.ok((select start_conversation(:'kemis')) = :'conv', 'start_conversation reopens the same chat');
insert into messages (conversation_id, sender_id, body) values (:'conv', auth.uid(), 'ሰላም, ኣሎ ድዩ?');
reset role; select t.act('00000000-0000-0000-0000-00000000000c'); set role authenticated;
select t.ok((select count(*) from messages) = 0, 'outsider cannot read chat');
select t.fails(format($$insert into messages (conversation_id, sender_id, body) values (%L, auth.uid(), 'spam')$$, :'conv'), 'row-level security', 'outsider cannot post in chat');

-- Reviews
select t.fails($$insert into reviews (reviewer_id, seller_id, rating) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 1)$$, 'chatted with or bought', 'stranger cannot review a seller');
reset role; select t.act('00000000-0000-0000-0000-00000000000b'); set role authenticated;
insert into reviews (reviewer_id, seller_id, rating, comment) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 5, 'ጽቡቕ');
select t.fails($$insert into reviews (reviewer_id, seller_id, rating) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 1)$$, 'reviews_one_per_listing', 'no duplicate review without listing');
select t.fails($$update reviews set seller_id = '00000000-0000-0000-0000-00000000000c'$$, 'permission denied', 'review cannot be moved to another seller');
update reviews set rating = 4;
select t.ok((select average_rating = 4.0 and review_count = 1 from seller_ratings where seller_id = '00000000-0000-0000-0000-00000000000a'), 'seller_ratings view');

-- Images
select t.fails(format($$insert into listing_images (listing_id, storage_path) values (%L, '00000000-0000-0000-0000-00000000000b/x.jpg')$$, :'phone'), 'row-level security', 'cannot attach images to others'' listings');
reset role; select t.act('00000000-0000-0000-0000-00000000000a'); set role authenticated;
select t.fails(format($$insert into listing_images (listing_id, storage_path) values (%L, '00000000-0000-0000-0000-00000000000b/x.jpg')$$, :'phone'), 'row-level security', 'image path must be in own folder');
insert into listing_images (listing_id, storage_path) values (:'phone', '00000000-0000-0000-0000-00000000000a/' || :'phone' || '/0.jpg');
select t.ok(true, 'seller attaches image from own folder');

-- Admin listing order per category
reset role;
create function t.feed(p_category text, q text default null) returns uuid[] language sql as $$
  select array_agg(s.id order by s.ordinality) from search_listings(q => q, p_category_id => p_category, p_country => 'ER') with ordinality s $$;
insert into auth.users (id) values ('00000000-0000-0000-0000-00000000000d');
insert into admins (user_id) values ('00000000-0000-0000-0000-00000000000d');
insert into listings (seller_id, category_id, listing_type, title, description, price_minor, currency, country_code, city, created_at) values
  ('00000000-0000-0000-0000-00000000000a', 'phones-smartphones', 'classified', 'Ordered Phone One', '', 100, 'ERN', 'ER', 'Asmara', now() - interval '5 days') returning id as s1 \gset
insert into listings (seller_id, category_id, listing_type, title, description, price_minor, currency, country_code, city, created_at) values
  ('00000000-0000-0000-0000-00000000000a', 'phones-smartphones', 'classified', 'Galaxy Phone Two', '', 100, 'ERN', 'ER', 'Asmara', now() - interval '4 days') returning id as s2 \gset
insert into listings (seller_id, category_id, listing_type, title, description, price_minor, currency, country_code, city, created_at) values
  ('00000000-0000-0000-0000-00000000000a', 'phones-accessories', 'classified', 'Ordered Case Three', 'fits galaxy', 100, 'ERN', 'ER', 'Asmara', now() - interval '3 days') returning id as s3 \gset
insert into listings (seller_id, category_id, listing_type, title, description, price_minor, currency, country_code, city, created_at) values
  ('00000000-0000-0000-0000-00000000000a', 'phones', 'classified', 'Middle Phone Five', '', 100, 'ERN', 'ER', 'Asmara', now() - interval '2 days') returning id as s5 \gset
insert into listings (seller_id, category_id, listing_type, title, description, price_minor, currency, country_code, city, created_at) values
  ('00000000-0000-0000-0000-00000000000a', 'phones-smartphones', 'classified', 'Newest Phone Four', '', 100, 'ERN', 'ER', 'Asmara', now() - interval '1 day') returning id as s4 \gset
insert into listings (seller_id, category_id, listing_type, title, description, price_minor, currency, country_code, city, created_at) values
  ('00000000-0000-0000-0000-00000000000a', 'habesha-wear-women', 'classified', 'Asmara Dress', '', 100, 'ERN', 'ER', 'Asmara', now() - interval '6 days') returning id as d1 \gset
select t.ok(t.feed('phones') = array[:'s4', :'s5', :'s3', :'s2', :'s1']::uuid[], 'no placements: newest first');

-- normal user B
select t.act('00000000-0000-0000-0000-00000000000b'); set role authenticated;
select t.fails($$select * from admins$$, 'permission denied', 'normal user cannot read admins');
select t.ok(not is_admin(), 'is_admin() is false for a normal user');
select t.fails(format($$insert into category_placements (category_id, listing_id, position) values ('phones', %L, 1)$$, :'s1'), 'permission denied', 'normal user cannot insert placements');
select t.fails($$update category_placements set position = 1$$, 'permission denied', 'normal user cannot update placements');
select t.fails($$delete from category_placements$$, 'permission denied', 'normal user cannot delete placements');
select t.fails(format($$select admin_set_category_order('phones', array[%L]::uuid[])$$, :'s1'), 'only admins', 'normal user cannot call admin_set_category_order');
select t.fails(format($$select admin_pin_listing('phones', %L)$$, :'s1'), 'only admins', 'normal user cannot call admin_pin_listing');
select t.fails(format($$select admin_unpin_listing('phones', %L)$$, :'s1'), 'only admins', 'normal user cannot call admin_unpin_listing');
select t.fails($$select * from admin_get_category_order('phones')$$, 'only admins', 'normal user cannot call admin_get_category_order');
reset role; set role anon;
select t.fails(format($$select admin_pin_listing('phones', %L)$$, :'s1'), 'permission denied', 'anon cannot call admin RPCs');
select t.fails($$select is_admin()$$, 'permission denied', 'anon cannot call is_admin');

-- admin D
reset role; select t.act('00000000-0000-0000-0000-00000000000d'); set role authenticated;
select t.ok(is_admin(), 'is_admin() is true for an admin');
select admin_set_category_order('phones', array[:'s3', :'s1']::uuid[]);
select t.fails(format($$select admin_set_category_order('phones', array[%L, %L]::uuid[])$$, :'s1', :'s1'), 'duplicate', 'set order rejects duplicates');
select t.fails(format($$select admin_set_category_order('phones', array[%L]::uuid[])$$, :'d1'), 'not an active listing', 'set order rejects listings from another category');
select t.fails(format($$select admin_set_category_order('phones', array[%L]::uuid[])$$, :'charger'), 'not an active listing', 'set order rejects inactive listings');
select t.fails(format($$select admin_set_category_order('no-such-cat', array[%L]::uuid[])$$, :'s1'), 'unknown category', 'set order rejects unknown category');
reset role; set role anon;
select t.ok(t.feed('phones') = array[:'s3', :'s1', :'s4', :'s5', :'s2']::uuid[], 'category: placed first in admin order, then newest first');
select t.ok((t.feed('phones', 'galaxy'))[1] = :'s2', 'with search text the best match still comes first');
select t.ok(t.feed('phones-smartphones') = array[:'s4', :'s2', :'s1']::uuid[], 'order of phones does not affect phones-smartphones');
reset role; set role authenticated;
select admin_set_category_order('phones-smartphones', array[:'s2']::uuid[]);
select admin_set_category_order('all', array[:'d1', :'s5']::uuid[]);
reset role; set role anon;
select t.ok(t.feed('phones-smartphones') = array[:'s2', :'s4', :'s1']::uuid[], 'sub-category has its own order');
select t.ok(t.feed('phones') = array[:'s3', :'s1', :'s4', :'s5', :'s2']::uuid[], 'order of phones-smartphones / all does not affect phones');
select t.ok(t.feed(null) = array[:'d1', :'s5', :'s4', :'s3', :'s2', :'s1']::uuid[], '''all'' order applies when no category');

reset role; set role authenticated;
select admin_pin_listing('phones', :'s4', 1);
select t.ok((select array_agg(listing_id order by position) = array[:'s4', :'s3', :'s1']::uuid[] and array_agg(position order by position) = '{1,2,3}'
             from admin_get_category_order('phones')), 'pin inserts at position and shifts others');
select admin_pin_listing('phones', :'s4', 3);
select t.ok((select array_agg(listing_id order by position) = array[:'s3', :'s1', :'s4']::uuid[] and array_agg(position order by position) = '{1,2,3}'
             from admin_get_category_order('phones')), 'pin moves an already placed listing');
select admin_unpin_listing('phones', :'s3');
select t.ok((select array_agg(listing_id order by position) = array[:'s1', :'s4']::uuid[] and array_agg(position order by position) = '{1,2}'
             from admin_get_category_order('phones')), 'unpin closes the gap');
select admin_pin_listing('phones', :'s1', 1, now() + interval '1 day');
select admin_set_category_order('phones', array[:'s4', :'s1']::uuid[]);
select t.ok((select pinned_until is not null from admin_get_category_order('phones') where listing_id = :'s1'), 'set order keeps pinned_until');
reset role;
update category_placements set pinned_until = now() - interval '1 hour' where category_id = 'phones' and listing_id = :'s1';
set role authenticated;
select t.ok((select is_expired from admin_get_category_order('phones') where listing_id = :'s1'), 'admin sees expired placement');
reset role; set role anon;
select t.ok(t.feed('phones') = array[:'s4', :'s5', :'s3', :'s2', :'s1']::uuid[], 'expired pinned_until is ignored');
reset role; update listings set status = 'sold' where id = :'s4'; set role anon;
select t.ok(t.feed('phones') = array[:'s5', :'s3', :'s2', :'s1']::uuid[], 'placement of a sold listing is ignored');
reset role; update listings set status = 'active' where id = :'s4';
update listings set category_id = 'habesha-wear-women' where id = :'s4'; set role anon;
select t.ok(t.feed('phones') = array[:'s5', :'s3', :'s2', :'s1']::uuid[], 'placement of a moved listing is ignored');
select t.ok(t.feed('habesha-wear-women') = array[:'s4', :'d1']::uuid[], 'moved listing is not placed in its new category');
reset role; set role authenticated;
select admin_set_category_order('phones', '{}');
select t.ok((select count(*) from admin_get_category_order('phones')) = 0, 'empty array clears the order');

reset role;
select t.ok((select count(*) from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'messages') = 1, 'messages published to realtime');
\pset tuples_only on
select case when pass then 'PASS ' else 'FAIL ' end || name || coalesce('  -> ' || case when not pass then detail end, '') from t.results order by n;
select format('%s passed, %s failed', count(*) filter (where pass), count(*) filter (where not pass)) from t.results;
