/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.provider;

import javax.ws.rs.FormParam;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.apollocurrency.aplwallet.api.dto.account.WhiteListedAccount;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class WhiteListedAccountConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.isAssignableFrom(WhiteListedAccount.class)) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == FormParam.class) {
                    return (ParamConverter<T>) new WhiteListedAccountConverterProvider.WhileListedAccountConverter(
                        ((FormParam) annotation).value(), new ObjectMapper());
                }
            }
        }
        return null;
    }

    @AllArgsConstructor
    public static class WhileListedAccountConverter implements ParamConverter<WhiteListedAccountList> {
        private String paramName;
        private ObjectMapper mapper;

        @Override
        public WhiteListedAccountList fromString(String value) {
            if (StringUtils.isBlank(value)) {
//                throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName, " should not be blank");
                return new WhiteListedAccountList(); // nothing to do
            }
            WhiteListedAccountList whiteListedAccountList;
            try {
                whiteListedAccountList = mapper.readValue(value, new TypeReference<>(){});
            } catch (JsonProcessingException e) {
                log.warn("Can't parse WhiteListedAccountList JSON data = {}", value);
                throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, paramName,
                    "'JSON' can't be parsed into 'WhiteListedAccountList', check if it's valid JSON and correct values");
            }
            return whiteListedAccountList;
        }

        @Override
        public String toString(WhiteListedAccountList value) {
            StringBuilder builder = new StringBuilder();
            for (WhiteListedAccount platformSpec : value.getList()) {
//                builder.append(platformSpec.getPlatform()).append("-").append(platformSpec.getArchitecture());
            }
            return builder.toString();
        }
    }

}
