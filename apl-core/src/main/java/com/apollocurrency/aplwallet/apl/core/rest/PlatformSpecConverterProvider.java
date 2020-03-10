package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.PlatformSpec;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.AllArgsConstructor;

import javax.ws.rs.FormParam;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

@Provider
public class PlatformSpecConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.isAssignableFrom(PlatformSpecs.class)) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == FormParam.class) {
                    return (ParamConverter<T>) new PlatformSpecConverter(((FormParam) annotation).value());
                }
            }
        }
        return null;
    }

    @AllArgsConstructor
    public static class PlatformSpecConverter implements ParamConverter<PlatformSpecs> {
        private String paramName;

        @Override
        public PlatformSpecs fromString(String value) {
            if (StringUtils.isBlank(value)) {
                throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, " should not be blank");
            }
            String[] platforms = value.split(",");
            PlatformSpecs platformSpecs = new PlatformSpecs();
            for (String spec : platforms) {
                String[] platformAndArch = spec.split("-");
                if (platformAndArch.length != 2) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, "value should consist of two platform params separated by hyphen, got " + spec);
                }
                Platform platform;
                try {
                    platform = Platform.valueOf(platformAndArch[0].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, "first part of value should be a platform like the following " + Arrays.toString(Platform.values()) + " got " + platformAndArch[0]);
                }
                Architecture architecture;
                try {
                    architecture = Architecture.valueOf(platformAndArch[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, "second part of value should be an architecture like the following " + Arrays.toString(Architecture.values()) + " got " + platformAndArch[1]);
                }
                platformSpecs.getSpecList().add(new PlatformSpec(platform, architecture));
            }
            return platformSpecs;
        }

        @Override
        public String toString(PlatformSpecs value) {
            StringBuilder builder = new StringBuilder();
            for (PlatformSpec platformSpec : value.getSpecList()) {
                builder.append(platformSpec.getPlatform()).append("-").append(platformSpec.getArchitecture());
            }
            return builder.toString();
        }
    }
}