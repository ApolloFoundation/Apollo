/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import static com.apollocurrency.aplwallet.apl.util.Constants.APPLICATION_DIR_NAME;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Class for mapping shard_id into shard file name as String.
 *
 * @author yuriy.larin
 */

public class ShardNameHelper {
    private static final Logger log = getLogger(ShardNameHelper.class);

    private final static String SHARD_NAME_PATTERN = APPLICATION_DIR_NAME + "-shard-%d-chain-%s";
    private final static String SHARD_ARCHIVE_NAME_PATTERN = APPLICATION_DIR_NAME + "-shard-%d-chain-%s.zip";

    public ShardNameHelper() {
    }
    
    public String getShardNameByShardId(Long shardId, UUID chainId) {
        if (shardId == null || shardId < 0) {
            throw new IllegalArgumentException("'shardId' should have positive value, but " + shardId + " was supplied");
        }
        Objects.requireNonNull(chainId, "chainID must be set");
        String result = String.format(SHARD_NAME_PATTERN, shardId, chainId.toString());
        log.debug(result);
        return result;
    }

    public String getShardArchiveNameByShardId(Long shardId, UUID chainId) {
        if (shardId == null || shardId < 0) {
            throw new IllegalArgumentException("'shardId' should have positive value, but " + shardId + " was supplied");
        }
        Objects.requireNonNull(chainId, "chainID must be set");
        String result = String.format(SHARD_ARCHIVE_NAME_PATTERN, shardId, chainId.toString());
        log.debug(result);
        return result;
    }

}
