/*
 * Copyright (c) 2022. Apollo Foundation.
 */
-- Recreate block_timestamp_idx with DESC sort to ensure
-- fast last block lookup required for popOff operation
--
-- Works only for index created from no-db initialization
--ALTER TABLE block  DROP CONSTRAINT  `block_timestamp_idx`;
-- Works only for index already populated with data
--DROP INDEX  block_timestamp_idx;
-- We just create a new index with reversed sort, because it's the only one
-- working solution to maintain existing db migration and new db creation
CREATE UNIQUE INDEX reversed_block_timestamp_idx ON block (`timestamp` DESC) ;