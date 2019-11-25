CREATE DATABASE apl_db; --where apl_db is a short hash of a db depending on a selected test net
CREATE USER sa WITH PASSWORD 'sa';
GRANT ALL PRIVILEGES ON DATABASE apl_db to sa; --where apl_db is a short hash of a db depending on a selected test net
