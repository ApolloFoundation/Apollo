/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.observer.events;

import javax.enterprise.util.AnnotationLiteral;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class AccountLedgerEventBinding extends AnnotationLiteral<AccountLedgerEvent> implements AccountLedgerEvent{

    public static AnnotationLiteral<AccountLedgerEvent> literal(final AccountLedgerEventType accountLedgerEventType) {
        return new AccountLedgerEventBinding() {
            @Override
            public AccountLedgerEventType value() {
                return accountLedgerEventType;
            }
        };
    }

}
