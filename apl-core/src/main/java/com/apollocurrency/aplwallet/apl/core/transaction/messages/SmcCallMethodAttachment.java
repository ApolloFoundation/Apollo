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
public class SmcCallMethodAttachment extends SmcAbstractAttachment {
    private static final String METHOD_NAME_FIELD = "contractSource";
    private static final String METHOD_PARAMS_FIELD = "params";
    private static final String AMOUNT_FIELD = "amount";

    private final String methodName;// method or constructor name
    private final List<String> methodParams;
    private final BigInteger amount;
    private final BigInteger fuelLimit;
    private final BigInteger fuelPrice;

    @Builder
    public SmcCallMethodAttachment(String methodName, List<String> methodParams, BigInteger amount, BigInteger fuelLimit, BigInteger fuelPrice) {
        this.methodName = Objects.requireNonNull(methodName);
        this.methodParams = Objects.requireNonNull(methodParams);
        this.amount = Objects.requireNonNull(amount);
        this.fuelLimit = Objects.requireNonNull(fuelLimit);
        this.fuelPrice = Objects.requireNonNull(fuelPrice);
    }

    public SmcCallMethodAttachment(RlpReader reader) {
        super(reader);
        this.methodName = reader.readString();
        this.methodParams = reader.readList(RlpConverter::toString);
        this.amount = reader.readBigInteger();
        this.fuelLimit = reader.readBigInteger();
        this.fuelPrice = reader.readBigInteger();
    }

    public SmcCallMethodAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.methodName = String.valueOf(attachmentData.get(METHOD_NAME_FIELD));
        this.methodParams = new ArrayList<>((JSONArray) attachmentData.get(METHOD_PARAMS_FIELD));
        this.amount = new BigInteger((String) attachmentData.get(AMOUNT_FIELD));
        this.fuelLimit = new BigInteger((String) attachmentData.get(FUEL_LIMIT_FIELD));
        this.fuelPrice = new BigInteger((String) attachmentData.get(FUEL_PRICE_FIELD));
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
        json.put(AMOUNT_FIELD, amount.toString());
        json.put(FUEL_LIMIT_FIELD, fuelLimit.toString());
        json.put(FUEL_PRICE_FIELD, fuelPrice.toString());
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        builder
            .add(methodName)
            .add(RlpList.ofStrings(methodParams))
            .add(amount)
            .add(fuelLimit)
            .add(fuelPrice);
    }

}
