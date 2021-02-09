/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpConverter;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Builder
@Getter
@AllArgsConstructor
public class SmcCallMethodAttachment extends SmcAbstractAttachment {
    private static final String METHOD_NAME_FIELD = "contractSource";
    private static final String METHOD_PARAMS_FIELD = "constructorParams";

    private final String methodName;
    private final List<String> methodParams;

    public SmcCallMethodAttachment(RlpReader reader) {
        super(reader);
        this.methodName = reader.readString();
        this.methodParams = reader.readList(RlpConverter::toString);
    }

    public SmcCallMethodAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.methodName = String.valueOf(attachmentData.get(METHOD_NAME_FIELD));
        this.methodParams = (List<String>) attachmentData.get(METHOD_PARAMS_FIELD);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(METHOD_NAME_FIELD, this.methodName);
        json.put(METHOD_PARAMS_FIELD, this.methodParams);
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        builder
            .add(methodName)
            .add(RlpList.ofStrings(methodParams));
    }

}
