/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 * 
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import javax.sql.DataSource;

public class ShardInitTableSchemaVersion extends DbVersion {

    protected int update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS BLOCK (" +
                        "DB_ID BIGINT not null, " +
                        "ID BIGINT not null, " +
                        "VERSION INTEGER not null, " +
                        "TIMESTAMP INTEGER not null, " +
                        "PREVIOUS_BLOCK_ID BIGINT, " +
                        "TOTAL_AMOUNT BIGINT not null, " +
                        "TOTAL_FEE BIGINT not null, " +
                        "PAYLOAD_LENGTH INTEGER not null, " +
                        "PREVIOUS_BLOCK_HASH binary(32), " +
                        "CUMULATIVE_DIFFICULTY binary not null, " +
                        "BASE_TARGET BIGINT not null, " +
                        "NEXT_BLOCK_ID BIGINT, " +
                        "HEIGHT INTEGER not null, " +
                        "GENERATION_SIGNATURE binary(64) not null, " +
                        "BLOCK_SIGNATURE binary(64) not null, " +
                        "PAYLOAD_HASH binary(32) not null, " +
                        "GENERATOR_ID BIGINT not null, " +
                        "TIMEOUT INTEGER default 0 not null " +
                        ")");
            case 2:
                apply("CREATE TABLE IF NOT EXISTS TRANSACTION (" +
                        "db_id BIGINT not null, " +
                        "id BIGINT NOT NULL, " +
                        "deadline SMALLINT NOT NULL, " +
                        "recipient_id BIGINT, " +
                        "transaction_index SMALLINT NOT NULL, " +
                        "amount BIGINT NOT NULL, " +
                        "fee BIGINT NOT NULL, " +
                        "full_hash BINARY(32) NOT NULL, " +
                        "height INT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
//                        "FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        "signature BINARY(64) NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "type TINYINT NOT NULL, " +
                        "subtype TINYINT NOT NULL, " +
                        "sender_id BIGINT NOT NULL, " +
                        "block_timestamp INT NOT NULL, " +
                        "referenced_transaction_full_hash BINARY(32), " +
                        "phased BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "attachment_bytes VARBINARY, " +
                        "version TINYINT NOT NULL, " +
                        "has_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "ec_block_height INT DEFAULT NULL, " +
                        "ec_block_id BIGINT DEFAULT NULL, " +
                        "has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_prunable_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_prunable_attachment BOOLEAN NOT NULL DEFAULT FALSE)");

            case 3:
                apply("CREATE TABLE IF NOT EXISTS public_key (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "public_key BINARY(32), height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 4:
                apply("CREATE TABLE IF NOT EXISTS referenced_transaction (db_id IDENTITY, transaction_id BIGINT NOT NULL, "
                        + "FOREIGN KEY (transaction_id) REFERENCES transaction (id) ON DELETE CASCADE, "
                        + "referenced_transaction_id BIGINT NOT NULL)");
            case 5:
                apply("CREATE TABLE IF NOT EXISTS genesis_public_key " +
                        "(db_id IDENTITY," +
                        "account_id BIGINT NOT NULL, " +
                        "public_key BINARY(32), " +
                        "height INT NOT NULL, " +
                        "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 6:
                apply("CREATE TABLE IF NOT EXISTS option (name VARCHAR(100) not null, value VARCHAR(250))");
            case 7:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS option_name_value_idx ON option(name, value)");
            case 8:
                return 8;
            default:
                throw new RuntimeException("Shard CREATE TABLES database is inconsistent with code, at update " + nextUpdate
                        + ", probably trying to run older code on newer database");
        }
    }

    @Override
    void init(DataSource dataSource) {
        super.init(dataSource);
    }

    @Override
    protected void apply(String sql) {
        super.apply(sql);
    }
}
