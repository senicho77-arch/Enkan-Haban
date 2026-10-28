-- =============================================================================
-- መበገሲ ሓበሬታ / Reference data: currencies and categories (Tigrinya + English)
-- This is a migration (not seed.sql) so it also reaches the live database.
-- =============================================================================

insert into public.currencies (code, name_ti, name_en, symbol, minor_units) values
  ('UGX', 'ሽልንግ ዩጋንዳ',      'Ugandan Shilling',   'USh', 0),
  ('ETB', 'ብር ኢትዮጵያ',        'Ethiopian Birr',     'Br',  2),
  ('ERN', 'ናቕፋ',              'Eritrean Nakfa',     'Nfk', 2),
  ('KES', 'ሽልንግ ኬንያ',        'Kenyan Shilling',    'KSh', 2),
  ('USD', 'ዶላር ኣመሪካ',        'US Dollar',          '$',   2),
  ('CAD', 'ዶላር ካናዳ',         'Canadian Dollar',    'C$',  2),
  ('AUD', 'ዶላር ኣውስትራልያ',     'Australian Dollar',  'A$',  2),
  ('EUR', 'ዩሮ',               'Euro',               '€',   2),
  ('GBP', 'ፓውንድ እንግሊዝ',       'British Pound',      '£',   2),
  ('AED', 'ድርሃም',             'UAE Dirham',         'AED', 2)
on conflict (code) do nothing;

insert into public.categories (id, parent_id, name_ti, name_en, icon, sort_order) values
  ('phones',       null, 'ሞባይላት',                'Phones',                  '📱', 1),
  ('electronics',  null, 'ኤሌክትሮኒክስ',             'Electronics',             '💻', 2),
  ('fashion',      null, 'ክዳውንቲ',                'Fashion',                 '👗', 3),
  ('habesha-wear', null, 'ባህላዊ ክዳን ሓበሻ',         'Habesha Traditional Wear', '🧣', 4),
  ('habesha-food', null, 'ቀመማትን ባህላዊ መግብን',      'Spices & Habesha Food',   '🌶️', 5),
  ('home',         null, 'ኣቑሑ ገዛ',               'Home & Furniture',        '🛋️', 6),
  ('vehicles',     null, 'መካይን',                 'Vehicles',                '🚗', 7),
  ('property',     null, 'ገዛውቲ',                 'Property',                '🏠', 8),
  ('beauty',       null, 'መመላኽዒ',                'Beauty',                  '💄', 9),
  ('groceries',    null, 'ኣስቤዛ',                 'Groceries',               '🛒', 10),
  ('kids',         null, 'ናይ ቆልዑ',               'Baby & Kids',             '🧸', 11),
  ('books',        null, 'መጻሕፍትን ሃይማኖታዊ ኣቑሑን',   'Books & Religious Items', '📖', 12),
  ('services',     null, 'ኣገልግሎታት',              'Services',                '🛠️', 13),
  ('jobs',         null, 'ስራሕ',                  'Jobs',                    '💼', 14),
  -- a few sub-categories to show how nesting works
  ('phones-smartphones',  'phones',       'ስማርት ፎን',     'Smartphones',        null, 1),
  ('phones-accessories',  'phones',       'ተወሰኽቲ ሞባይል',  'Phone Accessories',  null, 2),
  ('habesha-wear-women',  'habesha-wear', 'ዘሪያ / ቀሚሽ ደቂ ኣንስትዮ', 'Women''s Habesha Dress', null, 1),
  ('habesha-wear-men',    'habesha-wear', 'ክዳን ደቂ ተባዕትዮ',  'Men''s Traditional Wear', null, 2),
  ('property-rent',       'property',     'ንኽራይ',        'For Rent',           null, 1),
  ('property-sale',       'property',     'ንመሸጣ',        'For Sale',           null, 2)
on conflict (id) do nothing;
