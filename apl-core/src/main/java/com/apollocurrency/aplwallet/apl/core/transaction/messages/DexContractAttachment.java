package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import com.apollocurrency.aplwallet.apl.util.AplException;
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

    private String counterTransferTxId;
    /**
     * Encrypted secret key to have able to restore secret.
     */
    private byte[] encryptedSecret;

    /**
     * ExchangeContractStatus step_1/step_2 (0/1)
     */
    private ExchangeContractStatus contractStatus;

//    private Integer finishTime;

    public DexContractAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        this.orderId = buffer.getLong();
        this.counterOrderId = buffer.getLong();

        if (buffer.get() == 1) {
            byte hex[] = new byte[32];
            buffer.get(hex);
            this.secretHash = hex;
        }

        if (buffer.get() == 1) {
            byte encryptedSecretX[] = new byte[64];
            buffer.get(encryptedSecretX);
            this.encryptedSecret = encryptedSecretX;
        }

        this.contractStatus = ExchangeContractStatus.getType(buffer.get());

        try{
            this.setTransferTxId(Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH));
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }

        try {
            this.setCounterTransferTxId(Convert.emptyToNull(Convert.readString(buffer, buffer.getShort(), Constants.MAX_ADDRESS_LENGTH)));
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }


//        this.finishTime = buffer.getInt();
    }

    public DexContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("orderId")));
        this.counterOrderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("counterOrderId")));
        this.secretHash = attachmentData.get("secretHash") != null ? Convert.parseHexString(String.valueOf(attachmentData.get("secretHash"))) : null;
        this.encryptedSecret = attachmentData.get("encryptedSecret") != null ? Convert.parseHexString(String.valueOf(attachmentData.get("encryptedSecret"))) : null;
        this.contractStatus = ExchangeContractStatus.getType(Byte.valueOf(String.valueOf(attachmentData.get("contractStatus"))));
        this.transferTxId = String.valueOf(attachmentData.get("transferTxId"));
        this.transferTxId = String.valueOf(attachmentData.get("counterTransferTxId"));
//        this.finishTime = Integer.valueOf(String.valueOf(attachmentData.get("finishTime")));
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
                + 1 + (this.secretHash != null ? 32 : 0)
                + 1 + (this.encryptedSecret != null ? 64 : 0)
                + 1
                + 2 + Convert.toBytes(transferTxId).length
                + 2 + (this.counterTransferTxId != null ? Convert.toBytes(counterTransferTxId).length : 0)
//                + 4
                ;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.orderId);
        buffer.putLong(this.counterOrderId);

        //32
        if(this.secretHash != null) {
            buffer.put((byte) 1);
            buffer.put(this.secretHash);
        } else {
            buffer.put((byte) 0);
        }

        //64
        if(this.encryptedSecret != null) {
            buffer.put((byte) 1);
            buffer.put(this.encryptedSecret);
        } else {
            buffer.put((byte) 0);
        }

        buffer.put((byte) this.contractStatus.ordinal());

        byte[] transferTxId = Convert.toBytes(this.transferTxId);
        buffer.putShort((short) transferTxId.length);
        buffer.put(transferTxId);

        byte[] counterTransferTxId = this.counterTransferTxId != null ? Convert.toBytes(this.counterTransferTxId) : null;
        buffer.putShort(counterTransferTxId != null ? (short) counterTransferTxId.length : 0);
        if (counterTransferTxId != null) {
            buffer.put(counterTransferTxId);
        }

//        buffer.putInt(finishTime);

    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("orderId", Long.toUnsignedString(this.getOrderId()));
        json.put("counterOrderId", Long.toUnsignedString(this.getCounterOrderId()));
        if (this.secretHash != null) {
            json.put("secretHash", Convert.toHexString(this.secretHash));
        }
        if (this.encryptedSecret != null) {
            json.put("encryptedSecret", Convert.toHexString(this.encryptedSecret));
        }
        json.put("contractStatus", this.contractStatus.ordinal());
        json.put("transferTxId", this.transferTxId);
        json.put("counterTransferTxId", this.counterTransferTxId);
//        json.put("finishTime", this.finishTime);
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CONTRACT_TRANSACTION;
    }
}
