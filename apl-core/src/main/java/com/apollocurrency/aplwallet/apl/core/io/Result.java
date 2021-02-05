/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.io;

/**
 * The serialization result
 *
 * @author andrew.zinchenko@gmail.com
 */
public interface Result {

    byte[] array();

    /**
     * Returns the real size of the serialized transaction
     *
     * @return size
     */
    default int size() {
        return array().length;
    }

    /**
     * Returns the payload size of the serialized transaction.
     * As a rule the payload size equals the size except for prunable transactions.
     *
     * @return payload size
     */
    default int payloadSize() {
        return size();
    }

}
