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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

/**
 * Enum used for tracking migration state on all steps.
 */
public enum MigrateState {
    INIT, SHARD_DB_CREATED, DATA_MOVING_TO_SHARD_STARTED, DATA_MOVED_TO_SHARD,
    DATA_RELINKED_IN_MAIN, SECONDARY_INDEX_UPDATED, DATA_REMOVED_FROM_MAIN, COMPLETED, FAILED;
}
