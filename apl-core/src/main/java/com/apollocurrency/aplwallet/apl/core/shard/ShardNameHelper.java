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

import static com.apollocurrency.aplwallet.apl.util.Constants.APPLICATION_DIR_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Class for mapping shard_id into shard file name as String.
 *
 * @author yuriy.larin
 */
public class ShardNameHelper {
    private static final Logger log = getLogger(ShardNameHelper.class);

    private final static String SHARD_NAME_PATTERN = APPLICATION_DIR_NAME + "-shard-%07d";

    public static String getShardNameByShardId(Long shardId) {
        if (shardId == null || shardId < 0) {
            throw new RuntimeException("'shardId' should have positive value, but " + shardId + " was supplied");
        }
        String result = String.format(SHARD_NAME_PATTERN, shardId);
        log.debug(result);
        return result;
    }

}
