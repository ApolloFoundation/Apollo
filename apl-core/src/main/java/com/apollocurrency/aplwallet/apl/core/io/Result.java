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

    default int size() {
        return array().length;
    }

}
