CREATE TABLE provider_photos (
    provider_id bigint PRIMARY KEY REFERENCES provider_profiles(id),
    content_type varchar(30) NOT NULL,
    data bytea NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
