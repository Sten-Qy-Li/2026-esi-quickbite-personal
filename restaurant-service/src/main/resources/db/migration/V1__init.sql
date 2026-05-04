-- Restaurant Service V1 -- initial schema
CREATE TABLE restaurant (
    restaurant_id     UUID            PRIMARY KEY,
    owner_id          UUID            NOT NULL,
    name              VARCHAR(255)    NOT NULL,
    address           VARCHAR(255),
    city              VARCHAR(120),
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    operating_hours   VARCHAR(20),
    is_open           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP       NOT NULL
);

CREATE INDEX idx_restaurant_city  ON restaurant(city);
CREATE INDEX idx_restaurant_owner ON restaurant(owner_id);
