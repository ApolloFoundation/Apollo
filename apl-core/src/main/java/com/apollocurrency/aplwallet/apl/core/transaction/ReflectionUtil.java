/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class ReflectionUtil {
    public static Class<?> parametrizedClass(Class<?> clazz) {

        Type superclass = clazz.getGenericSuperclass();
        if (superclass instanceof ParameterizedType) {
            ParameterizedType genericSuperclass = (ParameterizedType) superclass;
            return ((Class<?>) genericSuperclass.getActualTypeArguments()[0]);
        } else {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            if (genericInterfaces.length != 1) {
                throw new IllegalArgumentException("Class " + clazz + " has 0 or more than 1 generic interfaces " + Arrays.toString(genericInterfaces));
            }
            Type genericInterface = genericInterfaces[0];
            ParameterizedType genericSuperclass = (ParameterizedType) genericInterface;
            return ((Class<?>) genericSuperclass.getActualTypeArguments()[0]);
        }

    }
}
