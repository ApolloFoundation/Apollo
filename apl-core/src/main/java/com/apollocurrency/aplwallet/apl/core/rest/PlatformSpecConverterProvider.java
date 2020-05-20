package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
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
                throw new RestParameterException(ApiErrors.INCORRECT_PARAM, paramName, " should not be blank");
            }
            String[] platforms = value.split(",");
            PlatformSpecs platformSpecs = new PlatformSpecs();
            for (String spec : platforms) {
                String[] platformAndArch = spec.split("-");
                if (platformAndArch.length != 2) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM, paramName, "value should consist of two platform params separated by hyphen, got " + spec);
                }
                OS os;
                try {
                    os = OS.from(platformAndArch[0].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM, paramName, "first part of value should be a platform like the following " + Arrays.toString(OS.values()) + " got " + platformAndArch[0]);
                }
                Arch architecture;
                try {
                    architecture = Arch.from(platformAndArch[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM, paramName, "second part of value should be an architecture like the following " + Arrays.toString(Arch.values()) + " got " + platformAndArch[1]);
                }
                platformSpecs.getSpecList().add(new PlatformSpec(os, architecture));
            }
            return platformSpecs;
        }

        @Override
        public String toString(PlatformSpecs value) {
            StringBuilder builder = new StringBuilder();
            for (PlatformSpec platformSpec : value.getSpecList()) {
                builder.append(platformSpec.getOS()).append("-").append(platformSpec.getArchitecture());
            }
            return builder.toString();
        }
    }
}