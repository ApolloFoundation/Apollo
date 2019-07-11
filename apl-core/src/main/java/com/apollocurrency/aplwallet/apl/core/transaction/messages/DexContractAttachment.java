package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Data
@Builder
@AllArgsConstructor
public class DexContractAttachment extends AbstractAttachment {

    private Long orderId;
    private Long counterOrderId;
    /**
     * Hash from secret key. sha256(key)
     */
    private byte[] secretHash;

    private String transferTxId;
    /**
     * Encrypted secret key to have able to restore secret.
     */
    private byte[] encryptedSecret;

    public DexContractAttachment(ByteBuffer buffer) throws NotValidException {
        super(buffer);
        this.orderId = buffer.getLong();
        this.counterOrderId = buffer.getLong();

        byte hex[] = new byte[32];
        buffer.get(hex);
        this.secretHash = hex;

        byte encryptedSecretX[] = new byte[64];
        buffer.get(encryptedSecretX);
        this.encryptedSecret = encryptedSecretX;

        this.setTransferTxId(Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH));
    }

    public DexContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("orderId")));
        this.counterOrderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("counterOrderId")));
        this.secretHash = Convert.parseHexString(String.valueOf(attachmentData.get("secretHash")));
        this.encryptedSecret = Convert.parseHexString(String.valueOf(attachmentData.get("encryptedSecret")));
        this.transferTxId = String.valueOf(attachmentData.get("transferTxId"));
    }

    @Override
    public int getMySize() {
        //secretHash fix size (hex(Sha256()) - 32 bites)
        return 8 + 8 + 32 + 64 + Convert.toBytes(transferTxId).length + 2;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.orderId);
        buffer.putLong(this.counterOrderId);
        buffer.put(this.secretHash);
        buffer.put(this.encryptedSecret);

        byte[] transferTxId = Convert.toBytes(this.transferTxId);
        buffer.putShort((short) transferTxId.length);
        buffer.put(transferTxId);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("orderId", this.getOrderId());
        json.put("counterOrderId", this.getCounterOrderId());
        json.put("secretHash",  Convert.toHexString(this.secretHash));
        json.put("encryptedSecret",  Convert.toHexString(this.encryptedSecret));
        json.put("transferTxId",  this.transferTxId);
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CONTRACT_TRANSACTION;
    }
}
