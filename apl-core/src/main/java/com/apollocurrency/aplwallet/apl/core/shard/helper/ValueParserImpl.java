/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Base64;

import static com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper.BYTE_ARRAY_PREFIX;

@Singleton
@Slf4j
public class ValueParserImpl implements ValueParser {
    private static final String WRONG_QUOTES_BALANCE = "Wrong quotes balance: [%s]";
    private static final String EOT_REGEXP = String.valueOf(CsvAbstractBase.EOT);
    private static final char QUOTE_CHAR = CsvAbstractBase.TEXT_FIELD_START;
    private static final String QUOTE = String.valueOf(CsvAbstractBase.TEXT_FIELD_START);
    private static final String DOUBLE_QUOTE = QUOTE + QUOTE;
    private final CsvEscaper translator;

    @Inject
    public ValueParserImpl(CsvEscaper translator) {
        this.translator = translator;
    }

    @Override
    public String parseStringObject(Object data) {
        String value = null;
        if (data != null) {
            String stringValue = removeQuote(data);
            value = stringValue.replaceAll(DOUBLE_QUOTE, QUOTE);
            value = translator.unEscape(value);
        }
        return value;
    }

    @Override
    public Object[] parseArrayObject(Object data) {
        Object[] actualArray = null;
        if (data != null) {
            String objectArray = (String) data;
            if (!StringUtils.isBlank(objectArray)) {
                String[] split = objectArray.split(EOT_REGEXP);
                actualArray = new Object[split.length];
                for (int j = 0; j < split.length; j++) {
                    String value = split[j];
                    if (value.startsWith(BYTE_ARRAY_PREFIX)) { //find byte arrays
                        //byte array found
                        actualArray[j] = parseBinaryObject(value);
                    } else if (value.startsWith(QUOTE)) { //find string
                        actualArray[j] = parseStringObject(value);
                    } else { // try to process number
                        try {
                            actualArray[j] = Integer.parseInt(split[j]);
                        } catch (NumberFormatException ignored) { // value can be of long type
                            try {
                                actualArray[j] = Long.parseLong(split[j]); // try to parse long
                            } catch (NumberFormatException e) { // throw exception, when specified value is not a string, long, int or byte array
                                throw new RuntimeException("Value " + split[j] + " of unsupported type");
                            }
                        }
                    }
                }
            } else {
                actualArray = new Object[0];
            }
        }
        return actualArray;
    }

    @Override
    public byte[] parseBinaryObject(Object data) {
        if (data == null) {
            return null;
        } else {
            String value = (String) data;
            if (value.startsWith(BYTE_ARRAY_PREFIX) && value.endsWith(QUOTE)) { //find byte arrays
                return Base64.getDecoder().decode(value.substring(2, value.length() - 1));
            } else {
                throw new RuntimeException("Expected byte array format: [b'...'], but found [" + value.substring(0, 2) + "]");
            }
        }
    }

    private String removeQuote(Object data) {
        String value = null;
        if (data != null) {
            String stringObject = (String) data;
            if (stringObject.charAt(0) == QUOTE_CHAR) {
                if (stringObject.charAt(stringObject.length() - 1) == QUOTE_CHAR) {
                    value = stringObject.substring(1, stringObject.length() - 1);
                } else {
                    throw new RuntimeException(String.format(WRONG_QUOTES_BALANCE, stringObject));
                }
            } else if (stringObject.charAt(stringObject.length() - 1) == QUOTE_CHAR) {
                throw new RuntimeException(String.format(WRONG_QUOTES_BALANCE, stringObject));
            } else {//string without quotes
                value = stringObject;
            }
        }
        return value;
    }
}
