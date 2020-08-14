/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import javax.enterprise.inject.Instance;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CachedTransactionTypeFactory implements TransactionTypeFactory {
    private Map<TypeSubtype, TransactionType> types = new HashMap<>();

    public CachedTransactionTypeFactory() {
        }


    private void putIfNotPresent(TransactionType type) {
        TransactionTypes.TransactionTypeSpec spec = type.getSpec();
        TypeSubtype typeSubtype = new TypeSubtype(spec.getType(), spec.getSubtype());
        if (types.containsKey(typeSubtype)) {
            throw new IllegalStateException("Duplicate instance for type: " + typeSubtype);
        }
        types.put(typeSubtype, type);
    }

    public void init(Instance<TransactionType> typeInstances) {
        for (TransactionType typeInstance : typeInstances) {
            putIfNotPresent(typeInstance);
        }

    }

    public CachedTransactionTypeFactory(Collection<TransactionType> transactionTypes) {
        for (TransactionType transactionType : transactionTypes) {
            putIfNotPresent(transactionType);
        }
    }

    @lombok.Data
    private static class TypeSubtype {
        private final int type;
        private final int subtype;
    }



    @Override
    public TransactionType findTransactionType(byte type, byte subtype) {
        return types.get(new TypeSubtype(type, subtype));
    }

    @Override
    public TransactionType findTransactionTypeBySpec(TransactionTypes.TransactionTypeSpec transactionTypeSpec) {
        return findTransactionType(transactionTypeSpec.getType(), transactionTypeSpec.getSubtype());
    }
}
