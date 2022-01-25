/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.api;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface Range<T extends Number> {

    boolean isTopBoundarySet();

    boolean isBottomBoundarySet();

    boolean inRange(T value);

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
     * Returns the min value of the range - the bottom boundary of the range
     *
     * @return the left boundary of the range
     */
    T from();

    default T bottom() {
        return from();
    }

    default T min() {
        return from();
    }

    /**
     * Adjust top boundary to specified value. Set top boundary to <code>value</code> if it equals to the <code>undefinedValue</code>.
     *
     * @param value the value for undefined boundary
     */
    T adjustTopBoundary(T value);

    /**
     * Adjust bottom boundary to specified value. Set bottom boundary to <code>value</code> if it equals to the <code>undefinedValue</code>.
     *
     * @param value          the value for undefined boundary
     */
    T adjustBottomBoundary(T value);

    /**
     * Returns the max value of the range - the top boundary of the range
     *
     * @return the right boundary of the range
     */
    T to();

    default T top() {
        return to();
    }

    default T max() {
        return to();
    }
}
