/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Objects;

public class GlobalObject<T> {
    private final T obj;
    private final String name;

    public GlobalObject(T obj, String name) {
        this.obj = obj;
        this.name = name;
    }

    public T getObj() {
        return obj;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GlobalObject)) return false;
        GlobalObject<?> that = (GlobalObject<?>) o;
        return Objects.equals(obj, that.obj) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj, name);
    }

    public boolean isValid() {
        return obj != null && name != null && !name.isEmpty();
    }
}
