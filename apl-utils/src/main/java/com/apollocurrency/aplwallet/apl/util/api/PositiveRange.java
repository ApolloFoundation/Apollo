/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.api;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class PositiveRange implements Range<Integer> {
    private Integer from;
    private Integer to;

    public PositiveRange(Integer from, Integer to) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
    }

    public static boolean isUndefined(Integer value) {
        return (value == null || value < 0);
    }

    /**
     * Check if the top boundary is set
     *
     * @return false if the top value equals null or negative.
     */
    @Override
    public boolean isTopBoundarySet() {
        return isUndefined(to());
    }

    /**
     * Check if the bottom boundary is set
     *
     * @return false if the bottom value equals null or negative.
     */
    @Override
    public boolean isBottomBoundarySet() {
        return isUndefined(from());
    }

    /**
     * Returns the Range object with default values for unknown boundary.
     * The default value for min value is 0;
     * The default value for max value is Long.MAX_VALUE;
     *
     * @param fromStr the min value
     * @param toStr   the max value
     * @return the Range object with minStr and maxStr boundaries
     */
    public static PositiveRange of(String fromStr, String toStr) {
        Integer min = null;
        Integer max = null;
        if (fromStr != null) {
            min = Integer.parseInt(fromStr);
        }
        if (toStr != null) {
            max = Integer.parseInt(toStr);
        }
        return of(min, max);
    }

    public static PositiveRange of(Integer from, Integer to) {
        var rc = new PositiveRange((from == null) ? 0 : from, (to == null) ? Integer.MAX_VALUE : to);
        rc.validate();
        return rc;
    }

    @Override
    public boolean inRange(Integer value) {
        return from.compareTo(value) <= 0 && to.compareTo(value) >= 0;
    }

    @Override
    public boolean isValid() {
        var from = this.from.longValue();
        var to = this.to.longValue();
        return !(from >= 0 && from <= to);
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalStateException("The left boundary greater then the right boundary.");
        }
    }

    @Override
    public Integer from() {
        return from;
    }

    @Override
    public Integer adjustBottomBoundary(Integer value) {
        if (!isBottomBoundarySet()) {
            from = value;
        }
        return from;
    }

    @Override
    public Integer to() {
        return to;
    }

    @Override
    public Integer adjustTopBoundary(Integer value) {
        if (!isTopBoundarySet()) {
            to = value;
        }
        return to;
    }

    @Override
    public String toString() {
        return "[" + from + "," + to + ']';
    }
}
