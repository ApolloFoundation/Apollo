/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpConverter;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpListBuilder;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
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
public class SmcPublishContractAttachment extends SmcAbstractAttachment {
    private static final String CONTRACT_SOURCE_FIELD = "contractSource";
    private static final String CONSTRUCTOR_PARAMS_FIELD = "constructorParams";

    private final String contractSource;
    private final List<String> constructorParams;

    public SmcPublishContractAttachment(byte[] input) {
        this(new RlpReader(input));
    }

    public SmcPublishContractAttachment(RlpReader reader) {
        super(reader);
        this.contractSource = reader.readString();
        this.constructorParams = reader.readList(RlpConverter::toString);
    }

    public SmcPublishContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.contractSource = String.valueOf(attachmentData.get(CONTRACT_SOURCE_FIELD));
        this.constructorParams= (List<String>) attachmentData.get(CONSTRUCTOR_PARAMS_FIELD);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(CONTRACT_SOURCE_FIELD, this.contractSource);
        json.put(CONSTRUCTOR_PARAMS_FIELD, this.constructorParams);
    }

    @Override
    public void putMyBytes(RlpWriteBuffer buffer) {
        buffer
            .write(contractSource)
            .write(RlpListBuilder.ofString(constructorParams));
    }


}
