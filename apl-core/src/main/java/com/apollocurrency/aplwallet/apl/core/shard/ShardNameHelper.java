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
            throw new IllegalArgumentException("'shardId' should have positive value, but " + shardId + " was supplied");
        }
        String result = String.format(SHARD_NAME_PATTERN, shardId);
        log.debug(result);
        return result;
    }

}
