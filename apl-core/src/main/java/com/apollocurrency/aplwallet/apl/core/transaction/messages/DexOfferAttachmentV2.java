package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOfferAttachmentV2 extends DexOfferAttachment {

    private String fromAddress;
    private String toAddress;

    public DexOfferAttachmentV2(DexOffer offer) {
        super(offer);
        this.fromAddress = offer.getFromAddress();
        this.toAddress = offer.getToAddress();
    }

    public DexOfferAttachmentV2(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try{
            this.fromAddress = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH);
            this.toAddress = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public DexOfferAttachmentV2(JSONObject attachmentData) {
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
        return Byte.valueOf("2");
    }

    @Override
    public String getAppendixName() {
        return "DexOrder_v2";
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
}
