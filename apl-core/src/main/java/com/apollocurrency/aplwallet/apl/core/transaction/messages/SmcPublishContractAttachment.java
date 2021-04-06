/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SmcPublishContractAttachment extends AbstractSmcAttachment {
    private static final String CONTRACT_NAME_FIELD = "name";
    private static final String CONTRACT_SOURCE_FIELD = "source";
    private static final String CONSTRUCTOR_PARAMS_FIELD = "params";
    private static final String LANGUAGE_FIELD = "language";

    private final String contractName;//contract name is a constructor name
    private final String contractSource;
    private final String constructorParams;//coma separated string of values
    private final String languageName;

    @Builder
    public SmcPublishContractAttachment(String contractName, String contractSource, String constructorParams, String languageName, BigInteger fuelLimit, BigInteger fuelPrice) {
        super(fuelLimit, fuelPrice);
        this.contractName = Objects.requireNonNull(contractName);
        this.contractSource = Objects.requireNonNull(contractSource);
        this.constructorParams = constructorParams;
        this.languageName = Objects.requireNonNull(languageName);
    }

    public SmcPublishContractAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.contractName = Convert.readString(buffer);
            this.contractSource = Convert.readString(buffer);
            this.constructorParams = Convert.readString(buffer);
            this.languageName = Convert.readString(buffer);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
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
        super.putMyJSON(json);
        json.put(CONTRACT_NAME_FIELD, this.contractName);
        json.put(CONTRACT_SOURCE_FIELD, this.contractSource);
        json.put(CONSTRUCTOR_PARAMS_FIELD, this.constructorParams);
        json.put(LANGUAGE_FIELD, languageName);
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        super.putMyBytes(builder);
        builder
            .add(contractName)
            .add(contractSource)
            .add(constructorParams)
            .add(languageName);
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        super.putMyBytes(buffer);
        Convert.writeString(buffer, contractName);
        Convert.writeString(buffer, contractSource);
        Convert.writeString(buffer, constructorParams);
        Convert.writeString(buffer, languageName);
    }

    @Override
    public int getMySize() {
        return Long.BYTES*2 + Integer.BYTES*4
            + contractName.getBytes(StandardCharsets.UTF_8).length
            + contractSource.getBytes(StandardCharsets.UTF_8).length
            + (constructorParams!=null?constructorParams.getBytes(StandardCharsets.UTF_8).length:0)
            + languageName.getBytes(StandardCharsets.UTF_8).length;
    }

}
