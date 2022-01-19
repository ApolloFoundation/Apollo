/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.api;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface Range {

    boolean inRange(Number value);

    /**
     * Validate the boundary.
     *
     * @return true if min value less than max value and false otherwise.
     */
    boolean isValid();

    /**
     * Validate the boundary.
     * <p>
     * Throws @{@link IllegalArgumentException} if min value greater than max value.
     *
     * @throws IllegalStateException if min value greater than max value.
     */
    void validate();

    /**
     * Returns the min value - the left boundary of the range
     *
     * @return the left boundary of the range
     */
    Number from();

    default Number min() {
        return from();
    }

    /**
     * Returns the max value - the right boundary of the range
     *
     * @return the right boundary of the range
     */
    Number to();

    default Number max() {
        return to();
    }
}
