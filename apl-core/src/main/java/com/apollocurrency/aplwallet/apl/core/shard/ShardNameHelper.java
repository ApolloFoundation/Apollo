/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.util.Constants.APPLICATION_DIR_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.util.Objects;
import java.util.UUID;

/**
 * Class for mapping shard_id into shard file name as String.
 *
 * @author yuriy.larin
 */

public class ShardNameHelper {
    private static final Logger log = getLogger(ShardNameHelper.class);

    private final static String SHARD_NAME_PATTERN = APPLICATION_DIR_NAME + "-shard-%d-chain-%s";
    private final static String SHARD_CORE_ARCHIVE_NAME_PATTERN = APPLICATION_DIR_NAME + "-shard-%d-chain-%s.zip";
    private final static String SHARD_PRUNABLE_ARCHIVE_NAME_PATTERN = APPLICATION_DIR_NAME + "-shardprun-%d-chain-%s.zip";
    private final static String SHARD_ID_PATTERN = "shard::%d;chain::%s";
    private final static String SHARD_PRUN_ID_PATTERN = "shardprun::%d;chain::%s";
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
    
    public String getFullShardId(Long shardId, UUID chainId){
       String result = String.format(SHARD_ID_PATTERN, shardId, chainId.toString());
       return result;
    }
    public String getFullShardPrunId(Long shardId, UUID chainId){
       String result = String.format(SHARD_PRUN_ID_PATTERN, shardId, chainId.toString());
       return result;
    }
    public String getCoreShardArchiveNameByShardId(Long shardId, UUID chainId) {
        return getShardArchiveNameByShardId(SHARD_CORE_ARCHIVE_NAME_PATTERN, shardId, chainId);
    }

    public String getPrunableShardArchiveNameByShardId(Long shardId, UUID chainId) {
        return getShardArchiveNameByShardId(SHARD_PRUNABLE_ARCHIVE_NAME_PATTERN, shardId, chainId);
    }

    private String getShardArchiveNameByShardId(String pattern, Long shardId, UUID chainId) {
        if (shardId == null || shardId < 0) {
            throw new IllegalArgumentException("'shardId' should have positive value, but " + shardId + " was supplied");
        }
        Objects.requireNonNull(chainId, "chainID must be set");
        String result = String.format(pattern, shardId, chainId.toString());
        log.debug(result);
        return result;
    }

}
