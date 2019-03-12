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

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class HelperFactoryImpl implements HelperFactory<BatchedSelectInsert> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BatchedSelectInsert> createHelper(String helperTableName) {
        if ("BLOCK".equalsIgnoreCase(helperTableName)) {
            return Optional.of(new BlockSelectAndInsertHelper());
        } else if ("TRANSACTION".equalsIgnoreCase(helperTableName)) {
            return Optional.of(new TransactionSelectAndInsertHelper());
        }
        return Optional.empty();
    }
}
