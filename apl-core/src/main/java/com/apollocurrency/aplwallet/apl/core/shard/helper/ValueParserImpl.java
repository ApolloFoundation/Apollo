/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvStringUtils;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Base64;

@Singleton
@Slf4j
public class ValueParserImpl implements ValueParser {
    private final static String EOT_REGEXP = String.valueOf(CsvAbstractBase.EOT);
    private final static char quoteChar = CsvAbstractBase.TEXT_FIELD_START;
    private final static String quote = String.valueOf(CsvAbstractBase.TEXT_FIELD_START);
    private final static String doubleQuote = quote+quote;

    @Override
    public String parseStringObject(Object data, char escape, char fieldDelimiter) {
        String value = null;
        if (data!=null) {
            String stringValue = removeQuote(data);
            value = stringValue.replaceAll(doubleQuote, quote);
            value = CsvStringUtils.unEscape(value, escape, fieldDelimiter);
        }
        return value;
    }

    @Override
    public String parseStringObject(Object data) {
        return parseStringObject(data, CsvStringUtils.DEFAULT_ESCAPE_CHARACTER, CsvStringUtils.DEFAULT_FIELD_DELIMITER);
    }

    @Override
    public Object[] parseArrayObject(Object data, char escape, char fieldDelimiter) {
        Object[] actualArray = null;
        if (data != null) {
            String objectArray = (String) data;
            if (!StringUtils.isBlank(objectArray)) {
                String[] split = objectArray.split(EOT_REGEXP);
                actualArray = new Object[split.length];
                for (int j = 0; j < split.length; j++) {
                    String value = split[j];
                    if (value.startsWith("b\'") && value.endsWith(quote)) { //find byte arrays
                        //byte array found
                        byte[] actualValue = Base64.getDecoder().decode(value.substring(2, value.length() - 1));
                        actualArray[j] = actualValue;
                    } else if (value.startsWith(quote)) { //find string
                        actualArray[j] = parseStringObject(value, escape, fieldDelimiter);
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
    public Object[] parseArrayObject(Object data) {
        return parseArrayObject(data, CsvStringUtils.DEFAULT_ESCAPE_CHARACTER, CsvStringUtils.DEFAULT_FIELD_DELIMITER);
    }

    @Override
    public byte[] parseBinaryObject(Object data) {
        if(data == null){
            return null;
        }else {
            return Base64.getDecoder().decode((removeQuote(data)));
        }
    }

    private String removeQuote(Object data) {
        String value = null;
        if (data!=null) {
            String stringObject = (String) data;
            if (stringObject.charAt(0) == quoteChar) {
                if (stringObject.charAt(stringObject.length() - 1) == quoteChar) {
                    value = stringObject.substring(1, stringObject.length() - 1);
                } else {
                    throw new RuntimeException("Wrong quotes balance: [" + stringObject + "]");
                }
            } else {//string without quotes
                value = stringObject;
            }
        }
        return value;
    }
}
