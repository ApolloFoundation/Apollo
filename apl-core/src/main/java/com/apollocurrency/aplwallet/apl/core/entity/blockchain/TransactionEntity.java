/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
@AllArgsConstructor
public class TransactionEntity {
    private long dbId;
    private long id;

    private short deadline;
    private long recipientId;
    private short index = -1;
    private long amountATM;
    private long feeATM;
    private byte[] fullHash;
    private int height;
    private long blockId;
    private int ecBlockHeight;
    private long ecBlockId;

    private byte[] signatureBytes;
    private int timestamp;
    private byte type;
    private byte subtype;
    private long senderId;
    private byte[] senderPublicKey;
    private int blockTimestamp = -1;
    private byte[] referencedTransactionFullHash;
    private byte version;

    private boolean hasMessage;
    private boolean hasEncryptedMessage;
    private boolean hasPublicKeyAnnouncement;
    private boolean hasEncryptToSelfMessage;
    private boolean phased;
    private boolean hasPrunableMessage;
    private boolean hasPrunableEencryptedMessage;
    private boolean hasPrunableAttachment;

    /* Serialized attachment and all appendages */
    private byte[] attachmentBytes;

    /**
     * Transaction V3 properties
     */
    /*
    private String chainId;
    private BigInteger nonce;
    private BigInteger amount;
    private BigInteger fuelLimit;
    private BigInteger fuelPrice;
     */

}
