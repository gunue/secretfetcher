-- SQL script to initialize the Postgres database and table

CREATE DATABASE test_db;

\c test_db;

CREATE TABLE test_table (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50),
    value VARCHAR(50)
);

-- Insert dummy data into the table

INSERT INTO test_table (name, value) VALUES ('dummy1', 'value1');
INSERT INTO test_table (name, value) VALUES ('dummy2', 'value2');
INSERT INTO test_table (name, value) VALUES ('dummy3', 'value3');

GRANT SELECT, INSERT, UPDATE, DELETE ON test_table TO postgres;

