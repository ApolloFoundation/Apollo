/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.expr;


import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Filters {

    public static Term orTerm(List<SmcEventSubscriptionRequest.Filter> filter) {
        List<Term> list = filter.stream().collect((Supplier<ArrayList<Term>>) ArrayList::new,
            (result, f) -> result.add(new EqTerm(f.getParameter(), f.getValue())),
            ArrayList::addAll);

        return new OrTerm(list);
    }

}
