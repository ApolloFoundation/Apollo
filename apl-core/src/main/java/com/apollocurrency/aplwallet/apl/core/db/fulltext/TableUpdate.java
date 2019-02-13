/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;
/**
 * Table update
 */
public class TableUpdate {

    /** Transaction thread */
    private final Thread thread;

    /** Old table row */
    private final Object[] oldRow;

    /** New table row */
    private final Object[] newRow;

    /**
     * Create the table update
     *
     * @param   thread          Transaction thread
     * @param   oldRow          Old table row or null
     * @param   newRow          New table row or null
     */
    public TableUpdate(Thread thread, Object[] oldRow, Object[] newRow) {
        this.thread = thread;
        this.oldRow = oldRow;
        this.newRow = newRow;
    }
    protected TableUpdate(Thread thread) {
        this.thread = thread;
        this.oldRow = new Object[]{};
        this.newRow = new Object[] {};
    }


    /**
     * Return the transaction thread
     *
     * @return                  Transaction thread
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * Return the old table row
     *
     * @return                  Old table row or null
     */
    public Object[] getOldRow() {
        return oldRow;
    }

    /**
     * Return the new table row
     *
     * @return                  New table row or null
     */
    public Object[] getNewRow() {
        return newRow;
    }
}
