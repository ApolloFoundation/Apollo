package com.apollocurrency.aplwallet.apl.core.app.observer.events;

import javax.enterprise.util.AnnotationLiteral;

public enum TxEventType {
    REMOVED_UNCONFIRMED_TRANSACTIONS,
    ADDED_UNCONFIRMED_TRANSACTIONS,
    ADDED_CONFIRMED_TRANSACTIONS,
    RELEASE_PHASED_TRANSACTION,
    REJECT_PHASED_TRANSACTION;

    public static AnnotationLiteral<TxEvent> literal(TxEventType type) {
        return new TxEventBinding() {
            @Override
            public TxEventType value() {
                return type;
            }
        };
    }
}