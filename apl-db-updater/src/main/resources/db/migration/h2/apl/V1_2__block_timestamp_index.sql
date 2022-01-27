/*
 * Copyright (c) 2022. Apollo Foundation.
 */
-- Recreate block_timestamp_idx with DESC sort to ensure
-- fast last block lookup required for popOff operation
DROP INDEX  block_timestamp_idx;
CREATE UNIQUE INDEX block_timestamp_idx ON block (`timestamp` DESC) ;