/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.api;

/**
 * @author Andrii Boiarskyi
 * @see
 * @since 1.0.0
 */
public class Sort {
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    private final String order;

    public Sort(String order) {
        this.order = order;
        if (!isASC() && !isDESC()) {
            throw new IllegalArgumentException("Not allowed sort value: " + order);
        }
    }

    /**
     * Returns the Sort object with default values for unknown ordering.
     * The default value is ASC.
     *
     * @param order the given order
     * @return the Sort object
     */
    public static Sort of(String order) {
        if (ASC.equalsIgnoreCase(order) || DESC.equalsIgnoreCase(order)) {
            return new Sort(order);
        } else {
            return asc();
        }
    }

    public boolean isASC() {
        return ASC.equalsIgnoreCase(order);
    }

    public boolean isDESC() {
        return DESC.equalsIgnoreCase(order);
    }

    @Override
    public String toString() {
        return order;
    }

    public static Sort desc() {
        return new Sort(DESC);
    }

    public static Sort asc() {
        return new Sort(ASC);
    }
}
