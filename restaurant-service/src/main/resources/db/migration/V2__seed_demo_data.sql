-- Restaurant Service V2 -- demo seed data (6 restaurants, Tartu + Tallinn).
-- Stable UUIDs per 0020-sten-contracts.md §5.1 so the Postman
-- collection and cross-service (Menu -> Restaurant) references line up.
-- Rows 3 and 6 are closed to support failure-path demos (Order Service
-- should reject an order against a closed restaurant).

INSERT INTO restaurant (restaurant_id, owner_id, name, address, city, latitude, longitude,
                        operating_hours, is_open, created_at, updated_at) VALUES
    ('d0000001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
     'Pizza Antonio', 'Ruutli 12',    'Tartu',   58.3776, 26.7290, '11:00-22:00', TRUE,
     TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:00:00'),
    ('d0000002-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
     'Sushi Lumi',    'Turu 3',        'Tartu',   58.3805, 26.7228, '12:00-23:00', TRUE,
     TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:00:00'),
    ('d0000003-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',
     'Cafe Nero',     'Vabaduse 6',    'Tartu',   58.3781, 26.7150, '08:00-20:00', FALSE,
     TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:00:00'),
    ('d0000004-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002',
     'Burger Bros',   'Viru 4',        'Tallinn', 59.4372, 24.7536, '10:00-22:00', TRUE,
     TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:00:00'),
    ('d0000005-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003',
     'Vegan Vibes',   'Parnu mnt 10',  'Tallinn', 59.4325, 24.7406, '11:00-21:00', TRUE,
     TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:00:00'),
    ('d0000006-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000003',
     'Pasta Palace',  'Roosikrantsi 2','Tallinn', 59.4328, 24.7459, '11:30-22:30', FALSE,
     TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:00:00')
ON CONFLICT (restaurant_id) DO NOTHING;
