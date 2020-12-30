/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.provider;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import lombok.AllArgsConstructor;

import javax.ws.rs.FormParam;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class ByteArrayConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.isAssignableFrom(byte[].class)) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == FormParam.class) {
                    return (ParamConverter<T>) new ByteArrayConverter(((FormParam) annotation).value());
                }
            }
        }
        return null;
    }

    @AllArgsConstructor
    public static class ByteArrayConverter implements ParamConverter<byte[]> {
        private String paramName;

        @Override
        public byte[] fromString(String value) {
            if (StringUtils.isBlank(value)) {
                throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, " should not be blank");
            }
            try {
                return Convert.parseHexString(value);
            } catch (NumberFormatException e) {
                throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, e.getMessage());
            }
        }

        @Override
        public String toString(byte[] value) {
            return Convert.toHexString(value);
        }
    }
}