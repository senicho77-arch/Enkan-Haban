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

-- Request call back
reset role; set role anon;
select t.fails(format($$select request_callback(%L, 'X', '+256700000000')$$, :'kemis'), 'permission denied', 'anon cannot request a call back');
reset role; select t.act('00000000-0000-0000-0000-00000000000b'); set role authenticated;
select t.fails(format($$select request_callback(%L, 'B', 'call me')$$, :'kemis'), 'phone', 'request_callback validates phone');
select t.fails(format($$select request_callback(%L, ' ', '+256700000000')$$, :'kemis'), 'name', 'request_callback requires a name');
select t.fails(format($$select request_callback(%L, 'B', '+256700000000')$$, :'phone'), 'not found', 'call back only on classified ads');
select request_callback(:'kemis', 'Buyer B', '+256 752 000 111') as cb \gset
select request_callback(:'kemis', 'Buyer B', '+256 752 000 222') as cb2 \gset
select t.ok(:'cb2' = :'cb' and (select buyer_phone from callback_requests where id = :'cb') = '+256 752 000 222', 'asking again updates the open request');
select t.fails(format($$insert into callback_requests (listing_id, buyer_id, seller_id, buyer_name, buyer_phone) values (%L, auth.uid(), '00000000-0000-0000-0000-00000000000a', 'B', '+256700000000')$$, :'kemis'), 'permission denied', 'callbacks only via request_callback');
update callback_requests set status = 'done' where id = :'cb';
select t.ok((select status from callback_requests where id = :'cb') = 'new', 'buyer cannot mark the call back done');
reset role; select t.act('00000000-0000-0000-0000-00000000000c'); set role authenticated;
select t.ok((select count(*) from callback_requests) = 0, 'outsider cannot see call back requests');
reset role; select t.act('00000000-0000-0000-0000-00000000000a'); set role authenticated;
select t.fails(format($$select request_callback(%L, 'A', '+256700000000')$$, :'kemis'), 'yourself', 'seller cannot request own call back');
select t.ok((select buyer_name from callback_requests where id = :'cb') = 'Buyer B', 'seller sees the call back request');
select t.fails(format($$update callback_requests set buyer_phone = '+1 000 000 0000' where id = %L$$, :'cb'), 'permission denied', 'seller cannot change the buyer''s number');
update callback_requests set status = 'done' where id = :'cb';
select t.ok((select status from callback_requests where id = :'cb') = 'done', 'seller marks the call back done');

reset role;
select t.ok((select count(*) from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'messages') = 1, 'messages published to realtime');
\pset tuples_only on
select case when pass then 'PASS ' else 'FAIL ' end || name || coalesce('  -> ' || case when not pass then detail end, '') from t.results order by n;
select format('%s passed, %s failed', count(*) filter (where pass), count(*) filter (where not pass)) from t.results;
