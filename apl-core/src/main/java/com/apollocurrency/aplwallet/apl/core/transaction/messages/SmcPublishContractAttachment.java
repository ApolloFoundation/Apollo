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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
    private final List<String> constructorParams;
    private final String languageName;
    private final BigInteger fuelLimit;//initial fuel limit for constructor call
    private final BigInteger fuelPrice;

    @Builder
    public SmcPublishContractAttachment(String contractName, String contractSource, List<String> constructorParams, String languageName, BigInteger fuelLimit, BigInteger fuelPrice) {
        this.contractName = Objects.requireNonNull(contractName);
        this.contractSource = Objects.requireNonNull(contractSource);
        this.constructorParams = Objects.requireNonNull(constructorParams);
        this.languageName = Objects.requireNonNull(languageName);
        this.fuelLimit = Objects.requireNonNull(fuelLimit);
        this.fuelPrice = Objects.requireNonNull(fuelPrice);
    }

    public SmcPublishContractAttachment(RlpReader reader) {
        super(reader);
        this.contractName = reader.readString();
        this.contractSource = reader.readString();
        this.constructorParams = reader.readList(RlpConverter::toString);
        this.languageName = reader.readString();
        this.fuelLimit = reader.readBigInteger();
        this.fuelPrice = reader.readBigInteger();
    }

    public SmcPublishContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.contractName = String.valueOf(attachmentData.get(CONTRACT_NAME_FIELD));
        this.contractSource = String.valueOf(attachmentData.get(CONTRACT_SOURCE_FIELD));
        this.constructorParams = new ArrayList<>((JSONArray) attachmentData.get(CONSTRUCTOR_PARAMS_FIELD));
        this.languageName = String.valueOf(attachmentData.get(LANGUAGE_FIELD));
        this.fuelLimit = new BigInteger((String) attachmentData.get(FUEL_LIMIT_FIELD));
        this.fuelPrice = new BigInteger((String) attachmentData.get(FUEL_PRICE_FIELD));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(CONTRACT_NAME_FIELD, this.contractName);
        json.put(CONTRACT_SOURCE_FIELD, this.contractSource);
        JSONArray params = new JSONArray();
        params.addAll(this.constructorParams);
        json.put(CONSTRUCTOR_PARAMS_FIELD, params);
        json.put(LANGUAGE_FIELD, languageName);
        json.put(FUEL_LIMIT_FIELD, fuelLimit.toString());
        json.put(FUEL_PRICE_FIELD, fuelPrice.toString());
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        builder
            .add(contractName)
            .add(contractSource)
            .add(RlpList.ofStrings(constructorParams))
            .add(languageName)
            .add(fuelLimit)
            .add(fuelPrice);
    }

}
