/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
public class TransactionEntity {
    private long dbId;
    private long id;

    private final int deadline;
    private final long recipientId;
    private short index = -1;
    private final long amountATM;
    private long feeATM;
    private byte[] fullHash;
    private volatile int height;
    private volatile long blockId;
    private final int ecBlockHeight;
    private final long ecBlockId;

    private byte[] signatureBytes;
    private final long timestamp;
    private final byte type;
    private final byte subtype;
    private long senderId;
    private byte[] senderPublicKey;
    private int blockTimestamp = -1;
    private final byte[] referencedTransactionFullHash;
    private final byte version;

    private boolean hasMessage;
    private boolean hasEncryptedMessage;
    private boolean hasPublicKeyAnnouncement;
    private boolean hasEncryptToSelfMessage;
    private boolean phased;
    private boolean hasPrunableMessage;
    private boolean hasPrunableEencryptedMessage;
    private boolean hasPrunableAttachment;

    /* Serialized attachment and all appendages */
    private final byte[] attachmentBytes;

    /**
     * Transaction V3 properties
     */
    private String chainId;
    private BigInteger nonce;
    private BigInteger amount;
    private BigInteger fuelLimit;
    private BigInteger fuelPrice;

}
