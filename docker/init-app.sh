#!/bin/sh
set -eu

# Give the API its own database owner without server-wide administrator rights.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 \
  --set app_password="$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE taskapp LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
ALTER DATABASE tasks OWNER TO taskapp;
SQL
