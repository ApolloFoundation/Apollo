/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.expr;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class OrTerm implements Term {
    private final List<? extends Predicate<Map<String, String>>> components;

    public OrTerm(List<? extends Predicate<Map<String, String>>> components) {
        this.components = components;
    }

    @Override
    public boolean test(Map<String, String> json) {
        // Avoid using the Iterator to avoid generating garbage (issue 820).
        for (Predicate<Map<String, String>> component : components) {
            if (component.test(json)) {
                return true;
            }
        }
        return false;
    }

}
