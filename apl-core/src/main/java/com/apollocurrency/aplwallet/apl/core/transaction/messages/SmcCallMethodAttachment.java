/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpConverter;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SmcCallMethodAttachment extends SmcAbstractAttachment {
    private static final String METHOD_NAME_FIELD = "contractMethod";
    private static final String METHOD_PARAMS_FIELD = "params";

    private final String methodName;// method or constructor name
    private final List<String> methodParams;

    @Builder
    public SmcCallMethodAttachment(String methodName, List<String> methodParams) {
        this.methodName = Objects.requireNonNull(methodName);
        this.methodParams = Objects.requireNonNull(methodParams);
    }

    public SmcCallMethodAttachment(RlpReader reader) {
        super(reader);
        this.methodName = reader.readString();
        this.methodParams = reader.readList(RlpConverter::toString);
    }

    public SmcCallMethodAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.methodName = String.valueOf(attachmentData.get(METHOD_NAME_FIELD));
        this.methodParams = new ArrayList<>((JSONArray) attachmentData.get(METHOD_PARAMS_FIELD));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(METHOD_NAME_FIELD, this.methodName);
        JSONArray params = new JSONArray();
        params.addAll(this.methodParams);
        json.put(METHOD_PARAMS_FIELD, params);
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        builder
            .add(methodName)
            .add(RlpList.ofStrings(methodParams));
    }

}
