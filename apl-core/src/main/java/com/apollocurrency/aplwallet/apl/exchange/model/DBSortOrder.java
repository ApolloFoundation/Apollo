package com.apollocurrency.aplwallet.apl.exchange.model;

public enum DBSortOrder {
    ASC("ASC"),
    DESC("DESC");

    String value;

    DBSortOrder(String value) {
        this.value = value;
    }

    public static DBSortOrder ordinal(int ordinal) {
        for (DBSortOrder sort : values()) {
            if (sort.ordinal() == ordinal) {
                return sort;
            }
        }
        throw new IllegalArgumentException("DexOrderSort was not found for code: " + ordinal);
    }

    public String getValue() {
        return value;
    }
}
