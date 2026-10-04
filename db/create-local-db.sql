-- Lokálna vývojová databáza pre Hodinového majstra.
-- Spúšťa sa ako superuser (postgres) cez setup-local-db.ps1, ktorý odovzdá premennú app_password.
-- Skript je opakovateľný: pri ďalšom spustení iba nastaví heslo a nič nezmaže.

SELECT 'CREATE ROLE fx_app LOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'fx_app') \gexec

ALTER ROLE fx_app WITH LOGIN PASSWORD :'app_password';

SELECT 'CREATE DATABASE fx OWNER fx_app ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fx') \gexec

REVOKE CONNECT ON DATABASE fx FROM PUBLIC;
GRANT CONNECT ON DATABASE fx TO fx_app;
