/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

/**
 * Exception is thrown when shard zip is not processed, unzip error, etc...
 */
public class ShardArchiveProcessingException extends RuntimeException {

    public ShardArchiveProcessingException(String message) {
        super(message);
    }

    public ShardArchiveProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
