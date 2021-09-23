/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.expr;


import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSubscriptionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SimpleFilter implements Term {
    private final Term term;

    public SimpleFilter(Term term) {
        this.term = term;
    }

    public static SimpleFilter from(List<SmcEventSubscriptionRequest.Filter> filter) {
        List<Term> list = filter.stream().collect((Supplier<ArrayList<Term>>) ArrayList::new,
            (result, f) -> result.add(new EqTerm(f.getParameter(), f.getValue())),
            ArrayList::addAll);

        return new SimpleFilter(new OrTerm(list));
    }

    @Override
    public boolean test(Map<String, String> json) {
        return term.test(json);
    }
}
