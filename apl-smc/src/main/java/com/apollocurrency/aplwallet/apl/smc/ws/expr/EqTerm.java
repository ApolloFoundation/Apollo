/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.expr;

import java.util.Map;
import java.util.Objects;


/**
 * @author andrew.zinchenko@gmail.com
 */
public class EqTerm extends BaseTerm {

    public EqTerm(String field, String value) {
        super(field, value);
    }

    @Override
    public boolean test(Map<String, String> json) {
        if (json.containsKey(field)) {
            var v = json.get(field);
            return Objects.equals(value, v);
        }
        return false;
    }
}
