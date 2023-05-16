/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

/**
 * @author Andrii Boiarskyi
 * @see
 * @since 1.0.0
 */
public class Sort {
    private final String order;

    public Sort(String order) {
        this.order = order;
        if (!isASC() && !isDESC()) {
            throw new IllegalArgumentException("Not allowed sort value: " + order);
        }
    }

    public boolean isASC() {
        return "ASC".equalsIgnoreCase(order);
    }

    public boolean isDESC() {
        return "DESC".equalsIgnoreCase(order);
    }

    @Override
    public String toString() {
        return order;
    }

    public static Sort desc() {
        return new Sort("DESC");
    }

    public static Sort asc() {
        return new Sort("ASC");
    }
}
