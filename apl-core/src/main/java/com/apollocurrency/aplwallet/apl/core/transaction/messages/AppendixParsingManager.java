/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;


import org.json.simple.JSONObject;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AppendixParsingManager {
    private final Map<Class<?>, AppendixParser<?>> parsers = new HashMap<>();

    @Inject
    public AppendixParsingManager(Instance<AppendixParser<?>> instances) {
        instances.iterator().forEachRemaining(e-> parsers.put(parameterClass(e.getClass()), e));
    }

    public AppendixParsingManager(Collection<AppendixParser<?>> parsers) {
        this.parsers.putAll(parsers.stream().collect(Collectors.toMap(e-> parameterClass(e.getClass()), Function.identity())));
    }

    private Class<?> parameterClass(Class<?> parametrizedClass) {
        return (Class<?>) ((ParameterizedType) parametrizedClass.getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public List<? extends Appendix> parseAppendices(JSONObject object) {
        return parsers.values()
            .stream()
            .map(p-> p.parse(object))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
