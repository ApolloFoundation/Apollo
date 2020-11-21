package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Builder
@Data
@AllArgsConstructor
public class DexContractAttachment extends AbstractAttachment {

    private Long orderId;
    private Long counterOrderId;
    /**
     * Hash from secret key. sha256(key)
     */
    private byte[] secretHash;

    private String transferTxId;

    private String counterTransferTxId;
    /**
     * Encrypted secret key to have able to restore secret.
     */
    private byte[] encryptedSecret;

    /**
     * ExchangeContractStatus step_1/step_2 (0/1)
     */
    private ExchangeContractStatus contractStatus;

    private Integer timeToReply;

    public DexContractAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        this.orderId = buffer.getLong();
        this.counterOrderId = buffer.getLong();

        if (buffer.get() == 1) {
            byte[] hex = new byte[32];
            buffer.get(hex);
            this.secretHash = hex;
            byte[] encryptedSecretX = new byte[64];
            buffer.get(encryptedSecretX);
            this.encryptedSecret = encryptedSecretX;
        }

        this.contractStatus = ExchangeContractStatus.getType(buffer.get());
        try {
            this.transferTxId = Convert.emptyToNull(Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH));
            this.counterTransferTxId = Convert.emptyToNull(Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH));
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }

        this.timeToReply = buffer.getInt();
    }

    public DexContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("orderId"));
        this.counterOrderId = Convert.parseUnsignedLong((String) attachmentData.get("counterOrderId"));
        this.secretHash = Convert.parseHexString((String) (attachmentData.get("secretHash")));
        this.encryptedSecret = Convert.parseHexString((String) attachmentData.get("encryptedSecret"));
        this.contractStatus = ExchangeContractStatus.values()[((Number) attachmentData.get("contractStatus")).intValue()];

        this.transferTxId = (String) attachmentData.get("transferTxId");
        this.counterTransferTxId = (String) attachmentData.get("counterTransferTxId");
        this.timeToReply = Integer.valueOf(String.valueOf(attachmentData.get("timeToReply")));
    }

    public DexContractAttachment(ExchangeContract exchangeContract) {
        this.orderId = exchangeContract.getOrderId();
        this.counterOrderId = exchangeContract.getCounterOrderId();
        this.secretHash = exchangeContract.getSecretHash();
        this.encryptedSecret = exchangeContract.getEncryptedSecret();
        this.contractStatus = exchangeContract.getContractStatus();
        this.transferTxId = exchangeContract.getTransferTxId();
        this.counterTransferTxId = exchangeContract.getCounterTransferTxId();
    }

    @Override
    public int getMySize() {
        return 8 + 8
            + 1 + (this.secretHash != null ? 32 + 64 : 0)
            + 1
            + 2 + (this.transferTxId != null ? Convert.toBytes(transferTxId).length : 0)
            + 2 + (this.counterTransferTxId != null ? Convert.toBytes(counterTransferTxId).length : 0)
            + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.orderId);
        buffer.putLong(this.counterOrderId);

        //32
        if (this.secretHash != null) {
            buffer.put((byte) 1);
            buffer.put(this.secretHash);
            buffer.put(this.encryptedSecret);
        } else {
            buffer.put((byte) 0);
        }

        buffer.put((byte) this.contractStatus.ordinal());

        byte[] transferTxId = this.transferTxId != null ? Convert.toBytes(this.transferTxId) : null;
        buffer.putShort(transferTxId != null ? (short) transferTxId.length : 0);
        if (transferTxId != null) {
            buffer.put(transferTxId);
        }

        byte[] counterTransferTxId = this.counterTransferTxId != null ? Convert.toBytes(this.counterTransferTxId) : null;
        buffer.putShort(counterTransferTxId != null ? (short) counterTransferTxId.length : 0);
        if (counterTransferTxId != null) {
            buffer.put(counterTransferTxId);
        }

        buffer.putInt(timeToReply);

    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("orderId", Long.toUnsignedString(this.getOrderId()));
        json.put("counterOrderId", Long.toUnsignedString(this.getCounterOrderId()));
        if (this.secretHash != null) {
            json.put("secretHash", Convert.toHexString(this.secretHash));
            json.put("encryptedSecret", Convert.toHexString(this.encryptedSecret));
        }
        json.put("contractStatus", this.contractStatus.ordinal());
        json.put("transferTxId", this.transferTxId);
        json.put("counterTransferTxId", this.counterTransferTxId);
        json.put("timeToReply", this.timeToReply);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DEX_CONTRACT;
    }
}
