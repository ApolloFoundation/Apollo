/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.observer.events;

import com.apollocurrency.aplwallet.apl.core.model.account.AccountEventType;

import javax.enterprise.util.AnnotationLiteral;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class AccountEventBinding extends AnnotationLiteral<AccountEvent> implements AccountEvent {

    public static AnnotationLiteral<AccountEvent> literal(final AccountEventType accountEventType) {
        return new AccountEventBinding() {
            @Override
            public AccountEventType value() {
                return accountEventType;
            }
        };
    }

}
