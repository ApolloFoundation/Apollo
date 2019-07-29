package com.apollocurrency.aplwallet.apl.util.task;

public enum TaskOrder {
    INIT(0),
    BEFORE(5),
    TASK(10),
    AFTER(20);

    private int order;

    TaskOrder(int order) {
        this.order = order;
    }

    public int getOrder(){
        return order;
    }
}
