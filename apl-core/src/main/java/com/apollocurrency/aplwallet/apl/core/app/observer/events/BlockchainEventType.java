/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.observer.events;

import javax.enterprise.util.AnnotationLiteral;

public enum BlockchainEventType {
    SUSPEND_DOWNLOADING,
    RESUME_DOWNLOADING,
    SHUTDOWN;

    public static AnnotationLiteral<BlockchainEvent> literal(BlockchainEventType type) {
        return new BlockchainEventBinding() {
            @Override
            public BlockchainEventType value() {
                return type;
            }
        };
    }
}