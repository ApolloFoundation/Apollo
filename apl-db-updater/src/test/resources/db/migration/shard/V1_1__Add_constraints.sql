
ALTER TABLE block ADD CONSTRAINT chk_timeout CHECK (timeout >= 0);

ALTER TABLE block ADD CONSTRAINT primary_key_block_id PRIMARY KEY (id);

ALTER TABLE block ADD CONSTRAINT block_timestamp_idx unique (`TIMESTAMP` DESC);

ALTER TABLE block ADD CONSTRAINT block_height_idx unique (height);

CREATE UNIQUE INDEX IF NOT EXISTS primary_key_block_db_id_index on block (db_id);

CREATE INDEX IF NOT EXISTS block_generator_id_idx on block (generator_id);

CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id);

CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height);

ALTER TABLE transaction ADD CONSTRAINT primary_key_transaction_db_id PRIMARY KEY (DB_ID);

CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id);

CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON transaction (sender_id);

CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id);

CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC);

ALTER TABLE transaction DROP CONSTRAINT IF EXISTS transaction_id_idx;

CREATE UNIQUE INDEX transaction_block_id_transaction_index_idx ON transaction (block_id, transaction_index);

