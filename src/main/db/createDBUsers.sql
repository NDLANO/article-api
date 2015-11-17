-- Schema
CREATE SCHEMA contentapi;

-- READONLY
CREATE USER contentapi_read with PASSWORD '<passord>';
ALTER DEFAULT PRIVILEGES IN SCHEMA contentapi GRANT SELECT ON TABLES TO contentapi_read;

GRANT CONNECT ON DATABASE data_test to contentapi_read;
GRANT USAGE ON SCHEMA contentapi to contentapi_read;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA contentapi TO contentapi_read;
GRANT SELECT ON ALL TABLES IN SCHEMA contentapi TO contentapi_read;

-- WRITE
CREATE USER contentapi_write with PASSWORD '<passord>';

GRANT CONNECT ON DATABASE data_test to contentapi_write;
GRANT USAGE ON SCHEMA contentapi to contentapi_write;
GRANT CREATE ON SCHEMA contentapi to contentapi_write;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA contentapi TO contentapi_write;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA contentapi TO contentapi_write;