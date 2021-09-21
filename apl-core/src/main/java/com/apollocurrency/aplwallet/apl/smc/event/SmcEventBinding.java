/*
 * Copyright © 2018-2021 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.event;

import javax.enterprise.util.AnnotationLiteral;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class SmcEventBinding extends AnnotationLiteral<SmcEvent> implements SmcEvent {

    public static AnnotationLiteral<SmcEvent> literal(final SmcEventType smcEventType) {
        return new SmcEventBinding() {
            @Override
            public SmcEventType value() {
                return smcEventType;
            }
        };
    }

}
