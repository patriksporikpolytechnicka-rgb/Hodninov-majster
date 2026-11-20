-- Average rating kept on the profile so the catalog can show and sort by it without extra queries.
ALTER TABLE provider_profiles ADD COLUMN IF NOT EXISTS rating_average double precision NOT NULL DEFAULT 0;
ALTER TABLE provider_profiles ADD COLUMN IF NOT EXISTS rating_count integer NOT NULL DEFAULT 0;
UPDATE provider_profiles p SET rating_count = s.c, rating_average = s.a
FROM (SELECT provider_id, count(*) AS c, avg(rating) AS a FROM reviews GROUP BY provider_id) s
WHERE s.provider_id = p.id;
