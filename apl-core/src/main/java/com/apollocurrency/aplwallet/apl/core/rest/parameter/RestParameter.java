/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;

public interface RestParameter<T> {

    /**
     * Return true if raw (not parsed) string parameter is null or empty
     * @return true if raw string parameter is null or empty
     */
    default boolean isEmpty(){
        return null == getRawData() || getRawData().isEmpty();
    }

    /**
     * Get raw data
     * @return extracted from request value
     */
    String getRawData();

    /**
     * Return parsed value or <code>null</code> if the raw string does not contain a parsable value. It's a silent Get.
     * @return parsed value or <code>null</code> if the raw string does not contain a parsable value.
     */
    T getIfPresent();

    /**
     * Get parsed value or throw exception if the raw string does not contain a parsable value.
     * @return parsed value
     * @throws RestParameterException - if the raw string does not contain a parsable value.
     */
    T get() throws RestParameterException;

    /**
     * Parse the string REST parameter as a value of type <code>T</code>.
     * @return non null parsed value
     * @throws RestParameterException - if the raw string does not contain a parsable value.
     */
    T parse() throws RestParameterException;

}
