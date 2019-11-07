/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.util.StringUtils;

import javax.inject.Singleton;
import java.util.Base64;

@Singleton
public class ValueParserImpl implements ValueParser {
    @Override
    public String parseStringObject(Object data) {
        String value = null;
        if (data!=null) {
            String stringObject = (String) data;
            String stringValue = null;
            if (stringObject.charAt(0) == '\'') {
                if (stringObject.charAt(stringObject.length() - 1) == '\'') {
                    stringValue = stringObject.substring(1, stringObject.length() - 1);
                } else {
                    throw new RuntimeException("Wrong quotes balance: [" + stringObject + "]");
                }
            } else {//string without quotes
                stringValue = stringObject;
            }
            value = stringValue.replaceAll("''", "'");
        }
        return value;
    }

    @Override
    public Object[] parseArrayObject(Object data) {
        Object[] actualArray = null;
        if (data != null) {
            String objectArray = (String) data;
            if (!StringUtils.isBlank(objectArray)) {
                String[] split = objectArray.split(",");
                actualArray = new Object[split.length];
                for (int j = 0; j < split.length; j++) {
                    String value = split[j];
                    if (value.startsWith("b\'") && value.endsWith("\'")) { //find byte arrays
                        //byte array found
                        byte[] actualValue = Base64.getDecoder().decode(value.substring(2, value.length() - 1));
                        actualArray[j] = actualValue;
                    } else if (value.startsWith("\'") && value.endsWith("\'")) { //find string
                        actualArray[j] = split[j].substring(1, split[j].length() - 1);
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
        if(data == null){
            return null;
        }else {
            return Base64.getDecoder().decode(((String) data));
        }
    }
}
