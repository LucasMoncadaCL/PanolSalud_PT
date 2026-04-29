CREATE OR REPLACE VIEW v_stock_summary AS
SELECT
    i.id            AS implement_id,
    i.name          AS implement_name,
    c.name          AS category,
    l.name          AS location,
    i.item_type,
    s.total_stock,
    s.min_stock,
    s.available,
    s.reserved,
    s.loaned,
    s.damaged,
    CASE
        WHEN s.loaned > 0 THEN 'Prestado'
        ELSE l.name
    END             AS display_location
FROM implement i
JOIN stock s ON s.implement_id = i.id
LEFT JOIN category c ON c.id = i.category_id
LEFT JOIN location l ON l.id = i.location_id
WHERE i.active = TRUE;
