#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-'EOSQL'
    SELECT 'CREATE DATABASE tenant1_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tenant1_db')\gexec

    SELECT 'CREATE DATABASE tenant2_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tenant2_db')\gexec

    SELECT 'CREATE DATABASE tenant3_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tenant3_db')\gexec
EOSQL
