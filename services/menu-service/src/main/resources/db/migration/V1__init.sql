-- Menu Service V1 -- initial schema
CREATE TABLE menu_item (
    menu_item_id      UUID            PRIMARY KEY,
    restaurant_id     UUID            NOT NULL,
    name              VARCHAR(255)    NOT NULL,
    description       VARCHAR(2000),
    price_amount      NUMERIC(19,2)   NOT NULL CHECK (price_amount > 0),
    price_currency    CHAR(3)         NOT NULL DEFAULT 'EUR',
    category          VARCHAR(100)    NOT NULL,
    is_available      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP       NOT NULL
);

CREATE INDEX idx_menu_item_restaurant ON menu_item(restaurant_id);
CREATE INDEX idx_menu_item_category   ON menu_item(category);
