package com.apollocurrency.aplwallet.apl.exchange.model;

public enum DexOrderSortBy {

    PAIR_RATE("pair_rate"),
    HEIGHT("db_id");

    String value;

    DexOrderSortBy(String value) {
        this.value = value;
    }

    public static DexOrderSortBy ordinal(int ordinal) {
        for (DexOrderSortBy sortBy : values()) {
            if (sortBy.ordinal() == ordinal) {
                return sortBy;
            }
        }
        throw new IllegalArgumentException("DexOrderSortBy was not found for code: " + ordinal);
    }


    public String getValue() {
        return value;
    }
}
