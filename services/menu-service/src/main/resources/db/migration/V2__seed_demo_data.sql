-- Menu Service V2 -- demo seed data (16 menu items across 6 restaurants).
-- Restaurant UUIDs match Restaurant Service V2 seed (see 0020 §5.1 and
-- §6: MenuItem.restaurantId is a cross-service reference, no FK).
-- Categories stick to the UI-suggested set (Appetizer/Main/Dessert/Drink)
-- per 0004 Q2 so dashboards group cleanly, but the DB column is still
-- free-form VARCHAR(100).

INSERT INTO menu_item (menu_item_id, restaurant_id, name, description,
                       price_amount, price_currency, category, is_available,
                       created_at, updated_at) VALUES
    -- Pizza Antonio
    ('e0000011-0000-0000-0000-000000000011', 'd0000001-0000-0000-0000-000000000001',
     'Margherita',       'Tomato, mozzarella, basil.',                 8.50, 'EUR', 'Main',      TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000012-0000-0000-0000-000000000012', 'd0000001-0000-0000-0000-000000000001',
     'Quattro Formaggi', 'Four cheese blend.',                        10.50, 'EUR', 'Main',      TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000013-0000-0000-0000-000000000013', 'd0000001-0000-0000-0000-000000000001',
     'Tiramisu',         'Mascarpone, coffee, cocoa.',                 5.00, 'EUR', 'Dessert',   TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000014-0000-0000-0000-000000000014', 'd0000001-0000-0000-0000-000000000001',
     'Garlic Bread',     'Toasted bread with garlic butter.',          3.50, 'EUR', 'Appetizer', TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    -- Sushi Lumi
    ('e0000021-0000-0000-0000-000000000021', 'd0000002-0000-0000-0000-000000000002',
     'Nigiri Set',       'Chef selection, 8 pieces.',                 14.00, 'EUR', 'Main',      TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000022-0000-0000-0000-000000000022', 'd0000002-0000-0000-0000-000000000002',
     'Miso Soup',        'Dashi, tofu, wakame.',                       4.00, 'EUR', 'Appetizer', TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000023-0000-0000-0000-000000000023', 'd0000002-0000-0000-0000-000000000002',
     'Mochi',            'Assorted mochi trio.',                       4.50, 'EUR', 'Dessert',   TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    -- Cafe Nero (restaurant closed, items stay listed)
    ('e0000031-0000-0000-0000-000000000031', 'd0000003-0000-0000-0000-000000000003',
     'Cappuccino',       'Double shot, steamed milk.',                 3.00, 'EUR', 'Drink',     TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000032-0000-0000-0000-000000000032', 'd0000003-0000-0000-0000-000000000003',
     'Chocolate Cake',   'Dark chocolate, 72% cocoa.',                 5.50, 'EUR', 'Dessert',   FALSE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    -- Burger Bros
    ('e0000041-0000-0000-0000-000000000041', 'd0000004-0000-0000-0000-000000000004',
     'Double Cheese',    'Two beef patties, cheddar, pickles.',        9.50, 'EUR', 'Main',      TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000042-0000-0000-0000-000000000042', 'd0000004-0000-0000-0000-000000000004',
     'Fries',            'Hand-cut, sea salt.',                        3.00, 'EUR', 'Appetizer', TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000043-0000-0000-0000-000000000043', 'd0000004-0000-0000-0000-000000000004',
     'Vanilla Shake',    'Vanilla bean ice cream, milk.',              4.00, 'EUR', 'Drink',     TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    -- Vegan Vibes
    ('e0000051-0000-0000-0000-000000000051', 'd0000005-0000-0000-0000-000000000005',
     'Quinoa Bowl',      'Quinoa, roasted veg, tahini.',              11.00, 'EUR', 'Main',      TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000052-0000-0000-0000-000000000052', 'd0000005-0000-0000-0000-000000000005',
     'Green Smoothie',   'Spinach, apple, ginger.',                    5.00, 'EUR', 'Drink',     TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    ('e0000053-0000-0000-0000-000000000053', 'd0000005-0000-0000-0000-000000000005',
     'Vegan Brownie',    'Black bean, maple, cocoa.',                  4.00, 'EUR', 'Dessert',   TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00'),
    -- Pasta Palace (restaurant closed, one item)
    ('e0000061-0000-0000-0000-000000000061', 'd0000006-0000-0000-0000-000000000006',
     'Carbonara',        'Guanciale, pecorino, egg yolk.',            10.00, 'EUR', 'Main',      TRUE,
     TIMESTAMP '2026-04-20 10:05:00', TIMESTAMP '2026-04-20 10:05:00')
ON CONFLICT (menu_item_id) DO NOTHING;
