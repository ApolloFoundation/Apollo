/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.api;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class NumericRange implements Range {
    private final Number from;
    private final Number to;

    public NumericRange(Number from, Number to) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
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
    public static NumericRange of(String fromStr, String toStr) {
        Number min = null;
        Number max = null;
        if (fromStr != null) {
            min = Long.parseLong(fromStr);
        }
        if (toStr != null) {
            max = Long.parseLong(toStr);
        }
        return of(min, max);
    }

    public static NumericRange of(Number from, Number to) {
        var rc = new NumericRange((from == null) ? 0 : from, (to == null) ? Long.MAX_VALUE : to);
        rc.validate();
        return rc;
    }

    @Override
    public boolean inRange(Number value) {
        return from.longValue() <= value.longValue() && value.longValue() <= to.longValue();
    }

    @Override
    public boolean isValid() {
        return !(from.longValue() > to.longValue());
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalStateException("The left boundary greater then the right boundary.");
        }
    }

    @Override
    public Number from() {
        return from;
    }

    @Override
    public Number to() {
        return to;
    }

    @Override
    public String toString() {
        return "[" + from + "," + to + ']';
    }
}
