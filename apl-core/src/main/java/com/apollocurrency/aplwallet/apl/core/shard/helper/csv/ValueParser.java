/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

public interface ValueParser {

    String parseStringObject(Object data);

    Object[] parseArrayObject(Object data);

    byte[] parseBinaryObject(Object data);

    default Object parseObject(Object data){
        return data;
    }
}
