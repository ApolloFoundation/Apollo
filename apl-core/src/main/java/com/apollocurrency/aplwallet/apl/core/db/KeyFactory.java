/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public abstract class KeyFactory<T> {
    
    private final String pkClause;
    private final String pkColumns;
    private final String selfJoinClause;

    protected KeyFactory(String pkClause, String pkColumns, String selfJoinClause) {
        this.pkClause = pkClause;
        this.pkColumns = pkColumns;
        this.selfJoinClause = selfJoinClause;
    }

    public abstract DbKey newKey(T t);

    public abstract DbKey newKey(ResultSet rs) throws SQLException;

    public T newEntity(DbKey dbKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public final String getPKClause() {
        return pkClause;
    }

    public final String getPKColumns() {
        return pkColumns;
    }

    // expects tables to be named a and b
    public final String getSelfJoinClause() {
        return selfJoinClause;
    }
    
}
