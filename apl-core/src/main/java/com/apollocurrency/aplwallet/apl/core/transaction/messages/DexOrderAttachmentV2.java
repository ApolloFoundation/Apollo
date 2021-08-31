/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Getter
@EqualsAndHashCode(callSuper = true)
public class DexOrderAttachmentV2 extends DexOrderAttachment {

    private final String fromAddress;
    private final String toAddress;

    public DexOrderAttachmentV2(DexOrder order) {
        super(order);
        this.fromAddress = order.getFromAddress();
        this.toAddress = order.getToAddress();
    }

    public DexOrderAttachmentV2(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.fromAddress = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH);
            this.toAddress = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public DexOrderAttachmentV2(JSONObject attachmentData) {
        super(attachmentData);
        this.fromAddress = String.valueOf(attachmentData.get("fromAddress"));
        this.toAddress = String.valueOf(attachmentData.get("toAddress"));
    }

    @Override
    public int getMySize() {
        return super.getMySize() + Convert.toBytes(fromAddress).length + 2 + Convert.toBytes(toAddress).length + 2;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);

        byte[] fromAddress = Convert.toBytes(this.fromAddress);
        buffer.putShort((short) fromAddress.length);
        buffer.put(fromAddress);

        byte[] toAddress = Convert.toBytes(this.toAddress);
        buffer.putShort((short) toAddress.length);
        buffer.put(toAddress);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        super.putMyJSON(json);
        json.put("fromAddress", this.fromAddress);
        json.put("toAddress", this.toAddress);
    }

    @Override
    public byte getVersion() {
        return 2;
    }

    @Override
    public boolean verifyVersion() {
        return this.getVersion() == 2;
    }

    @Override
    public String getAppendixName() {
        return "DexOrder_v2";
    }

}
