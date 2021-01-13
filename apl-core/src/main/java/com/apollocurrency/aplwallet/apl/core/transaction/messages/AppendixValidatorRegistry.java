/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Instance;
import javax.inject.Singleton;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class AppendixValidatorRegistry {
    private final Map<Class<?>, AppendixValidator<?>> validators = new HashMap<>();

    public AppendixValidatorRegistry() {

    }

    public AppendixValidatorRegistry(Collection<AppendixValidator<?>> validators) {
        validators.iterator().forEachRemaining(e-> this.validators.put((Class<?>) ((ParameterizedType)e.getClass().getGenericSuperclass()).getActualTypeArguments()[0], e));
    }

    public <T extends Appendix> AppendixValidator<T> getValidatorFor(T t) {
        return (AppendixValidator<T>) validators.get(t.getClass());
    }

    void init(Instance<AppendixValidator<?>> appendixValidators) {
       appendixValidators.iterator().forEachRemaining(e-> validators.put(ReflectionUtil.parametrizedClass(e.getClass()), e));
    }
}
