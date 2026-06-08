-- Runs once on first Postgres init (empty pgdata volume).
CREATE USER umami WITH PASSWORD 'umami';
CREATE DATABASE umami OWNER umami;
GRANT ALL PRIVILEGES ON DATABASE umami TO umami;
