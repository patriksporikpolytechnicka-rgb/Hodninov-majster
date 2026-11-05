-- Base categories for the school database. Run once with psql -f.
SET client_encoding = 'UTF8';

INSERT INTO categories (name)
VALUES ('Domácnosť'), ('Záhrada'), ('IT pomoc'), ('Sťahovanie')
ON CONFLICT (name) DO NOTHING;
