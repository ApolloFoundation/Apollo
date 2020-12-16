/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class SmcCallMethodAttachment extends SmcAbstractAttachment {

    private String methodName;
    private List<String> params;

    public SmcCallMethodAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        this(buffer.array());
    }

    public SmcCallMethodAttachment(byte[] input) throws AplException.NotValidException {
        this(new RlpReader(input));
    }

    public SmcCallMethodAttachment(RlpReader reader) throws AplException.NotValidException {
        super(1);
        this.methodName = reader.readString();
        params = new ArrayList<>();
        RlpReader paramsReader = reader.readList();
        while (paramsReader.hasNext()){
            params.add(paramsReader.readString());
        }
    }

    public SmcCallMethodAttachment(JSONObject attachmentData) {
        super(1);
        this.methodName = (String) attachmentData.get("methodName");
        this.params = (JSONArray) attachmentData.get("params");
    }

    @Override
    public int getMySize() {
        return 8 + 8 + 4;// TODO: calculate
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {


    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("methodName", getMethodName());
        json.put("params", getParams());
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }
}
