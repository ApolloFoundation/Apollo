/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.expr;


/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class BaseTerm implements Term {

    protected final String field;
    protected final String value;

    protected BaseTerm(String field, String value) {
        this.field = field;
        this.value = value;
    }
}
