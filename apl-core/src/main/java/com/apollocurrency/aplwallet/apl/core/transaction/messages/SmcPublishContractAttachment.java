/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SmcPublishContractAttachment extends SmcAbstractAttachment {
    private static final String CONTRACT_NAME_FIELD = "name";
    private static final String CONTRACT_SOURCE_FIELD = "source";
    private static final String CONSTRUCTOR_PARAMS_FIELD = "params";
    private static final String LANGUAGE_FIELD = "language";

    private final String contractName;//contract name is a constructor name
    private final String contractSource;
    private final String constructorParams;//coma separated string of values
    private final String languageName;

    @Builder
    public SmcPublishContractAttachment(String contractName, String contractSource, String constructorParams, String languageName) {
        this.contractName = Objects.requireNonNull(contractName);
        this.contractSource = Objects.requireNonNull(contractSource);
        this.constructorParams = constructorParams;
        this.languageName = Objects.requireNonNull(languageName);
    }

    public SmcPublishContractAttachment(RlpReader reader) {
        super(reader);
        this.contractName = reader.readString();
        this.contractSource = reader.readString();
        this.constructorParams = reader.readString();
        this.languageName = reader.readString();
    }

    public SmcPublishContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.contractName = String.valueOf(attachmentData.get(CONTRACT_NAME_FIELD));
        this.contractSource = String.valueOf(attachmentData.get(CONTRACT_SOURCE_FIELD));
        this.constructorParams = String.valueOf(attachmentData.get(CONSTRUCTOR_PARAMS_FIELD));
        this.languageName = String.valueOf(attachmentData.get(LANGUAGE_FIELD));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(CONTRACT_NAME_FIELD, this.contractName);
        json.put(CONTRACT_SOURCE_FIELD, this.contractSource);
        json.put(CONSTRUCTOR_PARAMS_FIELD, this.constructorParams);
        json.put(LANGUAGE_FIELD, languageName);
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        builder
            .add(contractName)
            .add(contractSource)
            .add(constructorParams)
            .add(languageName);
    }

}
