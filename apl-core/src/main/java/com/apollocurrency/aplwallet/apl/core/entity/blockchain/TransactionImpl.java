/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Encryptable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.transaction.AccountControl.SET_PHASING_ONLY;

public class TransactionImpl implements Transaction {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionImpl.class);

    private final short deadline;
    private final long recipientId;
    private final long amountATM;
    private final byte[] referencedTransactionFullHash;
    private final TransactionType type;
    private final int ecBlockHeight;
    private final long ecBlockId;
    private final byte version;
    private final int timestamp;
    private final AbstractAttachment attachment;
    private final MessageAppendix message;
    private final EncryptedMessageAppendix encryptedMessage;
    private final EncryptToSelfMessageAppendix encryptToSelfMessage;
    private final PublicKeyAnnouncementAppendix publicKeyAnnouncement;
    private final PhasingAppendix phasing;
    private final PrunablePlainMessageAppendix prunablePlainMessage;
    private final PrunableEncryptedMessageAppendix prunableEncryptedMessage;
    private final List<AbstractAppendix> appendages;
    private final int appendagesSize;
    private volatile byte[] senderPublicKey;
    private volatile long feeATM; // remove final modifier to set fee outside the class TODO get back 'final' modifier
    private Signature signature;
    private volatile int height;
    private volatile long blockId;
    private volatile Block block;
    private volatile int blockTimestamp = -1;
    private volatile short index = -1;
    private volatile long id;
    private volatile String stringId;
    private volatile long senderId;
    private volatile byte[] fullHash;
    private volatile byte[] bytes = null;
    private volatile long dbId;
    private volatile boolean hasValidSignature = false;

    private TransactionImpl(BuilderImpl builder) throws AplException.NotValidException {

        this.timestamp = builder.timestamp;
        this.deadline = builder.deadline;
        this.senderPublicKey = builder.senderPublicKey;
        this.recipientId = builder.recipientId;
        this.amountATM = builder.amountATM;
        this.referencedTransactionFullHash = builder.referencedTransactionFullHash;
        this.type = builder.type;
        this.version = builder.version;
        this.blockId = builder.blockId;
        this.height = builder.height;
        this.index = builder.index;
        this.id = builder.id;
        this.senderId = builder.senderId;
        this.blockTimestamp = builder.blockTimestamp;
        this.fullHash = builder.fullHash;
        this.ecBlockHeight = builder.ecBlockHeight;
        this.ecBlockId = builder.ecBlockId;
        this.dbId = builder.dbId;
        this.feeATM = builder.feeATM;
        List<AbstractAppendix> list = new ArrayList<>();
        if ((this.attachment = builder.attachment) != null) {
            list.add(this.attachment);
        }
        if ((this.message = builder.message) != null) {
            list.add(this.message);
        }
        if ((this.encryptedMessage = builder.encryptedMessage) != null) {
            list.add(this.encryptedMessage);
        }
        if ((this.publicKeyAnnouncement = builder.publicKeyAnnouncement) != null) {
            list.add(this.publicKeyAnnouncement);
        }
        if ((this.encryptToSelfMessage = builder.encryptToSelfMessage) != null) {
            list.add(this.encryptToSelfMessage);
        }
        if ((this.phasing = builder.phasing) != null) {
            list.add(this.phasing);
        }
        if ((this.prunablePlainMessage = builder.prunablePlainMessage) != null) {
            list.add(this.prunablePlainMessage);
        }
        if ((this.prunableEncryptedMessage = builder.prunableEncryptedMessage) != null) {
            list.add(this.prunableEncryptedMessage);
        }
        this.appendages = Collections.unmodifiableList(list);
        int appSize = 0;
        for (AbstractAppendix appendage : appendages) {
            appSize += appendage.getSize();
        }
        this.appendagesSize = appSize;
        this.signature = builder.signature;
    }

    public static TransactionImpl.BuilderImpl newTransactionBuilder(byte[] bytes) throws AplException.NotValidException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            //begin: read transaction header
            byte type = buffer.get();
            byte subtype = buffer.get();
            byte version = (byte) ((subtype & 0xF0) >> 4);
            subtype = (byte) (subtype & 0x0F);
            int timestamp = buffer.getInt();
            short deadline = buffer.getShort();
            byte[] senderPublicKey = new byte[32];
            buffer.get(senderPublicKey);
            long recipientId = buffer.getLong();
            long amountATM = buffer.getLong();
            long feeATM = buffer.getLong();
            byte[] referencedTransactionFullHash = new byte[32];
            buffer.get(referencedTransactionFullHash);
            referencedTransactionFullHash = Convert.emptyToNull(referencedTransactionFullHash);
            Signature signature = null;
            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            if (version < 2) {
                signature = signatureParser.parse(buffer);
            }
            int flags = 0;
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                flags = buffer.getInt();
                ecBlockHeight = buffer.getInt();
                ecBlockId = buffer.getLong();
            }
            //end: read transaction header
            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            //begin: read transaction attachment
            AbstractAttachment attachment = transactionType.parseAttachment(buffer);
            //end: read transaction attachment
            TransactionImpl.BuilderImpl builder = new BuilderImpl(
                version, senderPublicKey, amountATM, feeATM, deadline,
                attachment,
                timestamp)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            //begin: read transaction appendix
            int position = 1;
            if ((flags & position) != 0 || (version == 0 && transactionType == Messaging.ARBITRARY_MESSAGE)) {
                builder.appendix(new MessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new EncryptedMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PublicKeyAnnouncementAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new EncryptToSelfMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(PhasingAppendixFactory.build(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }
            //end: read transaction appendix
            if (version >= 2) {
                //read transaction multi-signature V2
                signature = signatureParser.parse(buffer);
            }
            builder.signature(signature);
            if (LOG.isTraceEnabled()) {
                LOG.trace("#MULTI_SIG# parsed signature={}", signature.getJsonString());
            }

            if (buffer.hasRemaining()) {
                throw new AplException.NotValidException("Transaction bytes too long, " + buffer.remaining() + " extra bytes");
            }
            return builder;
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse transaction bytes: " + Convert.toHexString(bytes));
            throw e;
        }
    }

    public static TransactionImpl.BuilderImpl newTransactionBuilder(byte[] bytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        BuilderImpl builder = newTransactionBuilder(bytes);
        if (prunableAttachments != null) {
            ShufflingProcessingAttachment shufflingProcessing = ShufflingProcessingAttachment.parse(prunableAttachments);
            if (shufflingProcessing != null) {
                builder.appendix(shufflingProcessing);
            }
            TaggedDataUploadAttachment taggedDataUploadAttachment = TaggedDataUploadAttachment.parse(prunableAttachments);
            if (taggedDataUploadAttachment != null) {
                builder.appendix(taggedDataUploadAttachment);
            }
            TaggedDataExtendAttachment taggedDataExtendAttachment = TaggedDataExtendAttachment.parse(prunableAttachments);
            if (taggedDataExtendAttachment != null) {
                builder.appendix(taggedDataExtendAttachment);
            }
            PrunablePlainMessageAppendix prunablePlainMessage = PrunablePlainMessageAppendix.parse(prunableAttachments);
            if (prunablePlainMessage != null) {
                builder.appendix(prunablePlainMessage);
            }
            PrunableEncryptedMessageAppendix prunableEncryptedMessage = PrunableEncryptedMessageAppendix.parse(prunableAttachments);
            if (prunableEncryptedMessage != null) {
                builder.appendix(prunableEncryptedMessage);
            }
        }
        return builder;
    }

    public static TransactionImpl.BuilderImpl newTransactionBuilder(JSONObject transactionData) throws AplException.NotValidException {
        try {
            byte type = ((Long) transactionData.get("type")).byteValue();
            byte subtype = ((Long) transactionData.get("subtype")).byteValue();
            int timestamp = ((Long) transactionData.get("timestamp")).intValue();
            short deadline = ((Long) transactionData.get("deadline")).shortValue();
            byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
            long amountATM = transactionData.containsKey("amountATM") ? Convert.parseLong(transactionData.get("amountATM")) : Convert.parseLong(transactionData.get("amountNQT"));
            long feeATM = transactionData.containsKey("feeATM") ? Convert.parseLong(transactionData.get("feeATM")) : Convert.parseLong(transactionData.get("feeNQT"));
            String referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
            Long versionValue = (Long) transactionData.get("version");
            byte version = versionValue == null ? 0 : versionValue.byteValue();
            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            JSONObject sigJsonObject;
            if (version < 2) {
                sigJsonObject = new JSONObject();
                sigJsonObject.put(SignatureParser.SIGNATURE_FIELD_NAME, transactionData.get("signature"));
            } else {
                sigJsonObject = (JSONObject) transactionData.get("signature");
            }
            Signature signature = signatureParser.parse(sigJsonObject);
            JSONObject attachmentData = (JSONObject) transactionData.get("attachment");
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                ecBlockHeight = ((Long) transactionData.get("ecBlockHeight")).intValue();
                ecBlockId = Convert.parseUnsignedLong((String) transactionData.get("ecBlockId"));
            }
            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            if (transactionType == null) {
                throw new AplException.NotValidException("Invalid transaction type: " + type + ", " + subtype);
            }
            TransactionImpl.BuilderImpl builder = new BuilderImpl(version, senderPublicKey,
                amountATM, feeATM, deadline,
                transactionType.parseAttachment(attachmentData), timestamp)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
                builder.recipientId(recipientId);
            }
            if (attachmentData != null) {
                builder.appendix(MessageAppendix.parse(attachmentData));
                builder.appendix(EncryptedMessageAppendix.parse(attachmentData));
                builder.appendix(PublicKeyAnnouncementAppendix.parse(attachmentData));
                builder.appendix(EncryptToSelfMessageAppendix.parse(attachmentData));
                builder.appendix(PhasingAppendixFactory.parse(attachmentData));
                builder.appendix(PrunablePlainMessageAppendix.parse(attachmentData));
                builder.appendix(PrunableEncryptedMessageAppendix.parse(attachmentData));
            }
            //set parsed signature
            builder.signature(signature);

            return builder;
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse transaction={}", transactionData.toJSONString());
            throw e;
        }
    }

    @Override
    public short getDeadline() {
        return deadline;
    }

    @Override
    public boolean shouldSavePublicKey() {
        return true;
        //        return Account.getPublicKey(senderId) == null;
    }

    @Override
    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public long getAmountATM() {
        return amountATM;
    }

    @Override
    public long getFeeATM() {
        return feeATM;
    }

    @Override
    public void setFeeATM(long feeATM) {
        this.feeATM = feeATM;
    }

    public long[] getBackFees() {
        return type.getBackFees(this);
    }

    @Override
    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return Convert.toHexString(referencedTransactionFullHash);
    }

    @Override
    public byte[] referencedTransactionFullHash() {
        return referencedTransactionFullHash;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void sign(Signature signature) {
        this.signature = signature;
        this.bytes = null;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public long getBlockId() {
        return blockId;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.blockTimestamp = block.getTimestamp();
    }

    public void unsetBlock() {
        this.block = null;
        this.blockId = 0;
        this.blockTimestamp = -1;
        this.index = -1;
        // must keep the height set, as transactions already having been included in a popped-off block before
        // get priority when sorted for inclusion in a new block
    }

    @Override
    public short getIndex() {
        if (index == -1) {
            throw new IllegalStateException("Transaction index has not been set");
        }
        return index;
    }

    public void setIndex(int index) {
        this.index = (short) index;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    @Override
    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    @Override
    public AbstractAttachment getAttachment() {
        attachment.loadPrunable(this);
        return attachment;
    }

    @Override
    public List<AbstractAppendix> getAppendages() {
        return getAppendages(false);
    }

    @Override
    public List<AbstractAppendix> getAppendages(boolean includeExpiredPrunable) {
        for (AbstractAppendix appendage : appendages) {
            appendage.loadPrunable(this, includeExpiredPrunable);
        }
        return appendages;
    }

    @Override
    public List<AbstractAppendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {
        List<AbstractAppendix> result = new ArrayList<>();
        appendages.forEach(appendix -> {
            if (filter.test(appendix)) {
                (appendix).loadPrunable(this, includeExpiredPrunable);
                result.add(appendix);
            }
        });
        return result;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (signature == null) {
                throw new IllegalStateException("Transaction is not signed yet");
            }
            byte[] data = getUnsignedBytes();
            byte[] signatureHash = Crypto.sha256().digest(signature.bytes());
            MessageDigest digest = Crypto.sha256();
            digest.update(data);
            fullHash = digest.digest(signatureHash);
            BigInteger bigInteger = new BigInteger(1, new byte[]{fullHash[7], fullHash[6], fullHash[5], fullHash[4], fullHash[3], fullHash[2], fullHash[1], fullHash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Long.toUnsignedString(id);
            }
        }
        return stringId;
    }

    @Override
    public String getFullHashString() {
        return Convert.toHexString(getFullHash());
    }

    @Override
    public byte[] getFullHash() {
        if (fullHash == null) {
            getId();
        }
        return fullHash;
    }

    @Override
    public long getSenderId() {
        if (senderId == 0) {
            senderId = AccountService.getId(getSenderPublicKey());
        }
        return senderId;
    }

    @Override
    public String toString() {
        return String.valueOf(getId());
    }

    @Override
    public MessageAppendix getMessage() {
        return message;
    }

    @Override
    public EncryptedMessageAppendix getEncryptedMessage() {
        return encryptedMessage;
    }

    @Override
    public EncryptToSelfMessageAppendix getEncryptToSelfMessage() {
        return encryptToSelfMessage;
    }

    @Override
    public PhasingAppendix getPhasing() {
        return phasing;
    }

    public boolean attachmentIsPhased() {
        return attachment.isPhased(this);
    }

    @Override
    public PublicKeyAnnouncementAppendix getPublicKeyAnnouncement() {
        return publicKeyAnnouncement;
    }

    @Override
    public PrunablePlainMessageAppendix getPrunablePlainMessage() {
        if (prunablePlainMessage != null) {
            prunablePlainMessage.loadPrunable(this);
        }
        return prunablePlainMessage;
    }

    public boolean hasPrunablePlainMessage() {
        return prunablePlainMessage != null;
    }

    @Override
    public PrunableEncryptedMessageAppendix getPrunableEncryptedMessage() {
        if (prunableEncryptedMessage != null) {
            prunableEncryptedMessage.loadPrunable(this);
        }
        return prunableEncryptedMessage;
    }

    public boolean hasPrunableEncryptedMessage() {
        return prunableEncryptedMessage != null;
    }

    public byte[] bytes() {
        if (bytes == null) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put(type.getType());
                buffer.put((byte) ((version << 4) | type.getSubtype()));
                buffer.putInt(timestamp);
                buffer.putShort(deadline);
                buffer.put(getSenderPublicKey());
                buffer.putLong(type.canHaveRecipient() ? recipientId : GenesisImporter.CREATOR_ID);
                buffer.putLong(amountATM);
                buffer.putLong(feeATM);
                if (referencedTransactionFullHash != null) {
                    buffer.put(referencedTransactionFullHash);
                } else {
                    buffer.put(new byte[32]);
                }
                if (version < 2) {
                    if (signature != null) {
                        buffer.put(signature.bytes());
                    } else {
                        buffer.put(new byte[Signature.ECDSA_SIGNATURE_SIZE]);
                    }
                }
                buffer.putInt(getFlags());
                buffer.putInt(ecBlockHeight);
                buffer.putLong(ecBlockId);
                for (Appendix appendage : appendages) {
                    appendage.putBytes(buffer);
                }
                if (version >= 2) {
                    if (signature != null) {
                        buffer.put(signature.bytes());
                    }
                }
                bytes = buffer.array();
            } catch (RuntimeException e) {
                if (signature != null && LOG.isDebugEnabled()) {
                    LOG.debug("Failed to get transaction bytes for transaction: {}", getJSONObject().toJSONString());
                }
                throw e;
            }
        }
        return bytes;
    }

    @Override
    public byte[] getUnsignedBytes() {
        if (version < 2) {
            return zeroSignature(getCopyTxBytes());
        } else {
            byte[] txBytes = bytes();
            return Arrays.copyOf(txBytes, txV2HeaderSize() + appendagesSize);
        }
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("id", Long.toUnsignedString(id));
        json.put("type", type.getType());
        json.put("subtype", type.getSubtype());
        json.put("timestamp", timestamp);
        json.put("deadline", deadline);
        json.put("senderPublicKey", Convert.toHexString(getSenderPublicKey()));
        if (type.canHaveRecipient()) {
            json.put("recipient", Long.toUnsignedString(recipientId));
        }
        json.put("amountATM", amountATM);
        json.put("feeATM", feeATM);
        if (referencedTransactionFullHash != null) {
            json.put("referencedTransactionFullHash", Convert.toHexString(referencedTransactionFullHash));
        }
        json.put("ecBlockHeight", ecBlockHeight);
        json.put("ecBlockId", Long.toUnsignedString(ecBlockId));
        JSONObject attachmentJSON = new JSONObject();
        for (AbstractAppendix appendage : appendages) {
            appendage.loadPrunable(this);
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (!attachmentJSON.isEmpty()) {
            json.put("attachment", attachmentJSON);
        }

        if (signature != null) {
            if (version < 2) {
                //json.put("signature", getSignature().getJsonObject().get(SignatureParser.SIGNATURE_FIELD_NAME));
                json.putAll(getSignature().getJsonObject());
            } else {
                json.put("signature", getSignature().getJsonObject());
            }
        }
        return json;
    }

    @Override
    public JSONObject getPrunableAttachmentJSON() {
        JSONObject prunableJSON = null;
        for (AbstractAppendix appendage : appendages) {
            if (appendage instanceof Prunable) {
                appendage.loadPrunable(this);
                if (prunableJSON == null) {
                    prunableJSON = appendage.getJSONObject();
                } else {
                    prunableJSON.putAll(appendage.getJSONObject());
                }
            }
        }
        return prunableJSON;
    }

    @Override
    public int getECBlockHeight() {
        return ecBlockHeight;
    }

    @Override
    public long getECBlockId() {
        return ecBlockId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransactionImpl && this.getId() == ((Transaction) o).getId();
    }

    @Override
    public byte[] getSenderPublicKey() {
        return senderPublicKey;
    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    private int getSize() {
        int signatureSize = 0;
        if (version < 2) {
            signatureSize = Signature.ECDSA_SIGNATURE_SIZE;
        } else {
            if (signature != null) {
                signatureSize = signature.getSize();
            }
        }
        return txV2HeaderSize() + appendagesSize + signatureSize;
    }

    @Override
    public int getFullSize() {
        int fullSize = getSize() - appendagesSize;
        for (AbstractAppendix appendage : getAppendages()) {
            fullSize += appendage.getFullSize();
        }
        return fullSize;
    }

    /**
     * The transaction V2 header size, it doesn't contain the signature size
     *
     * @return the transaction V2 header size
     */
    private int txV2HeaderSize() {
        return 1 + 1 + 4 + 2 + 32 + 8 + 8 + 8 + 32 + 4 + 4 + 8;
    }

    private int signatureV1Offset() {
        return 1 + 1 + 4 + 2 + 32 + 8 + 8 + 8 + 32;
    }

    private byte[] zeroSignature(byte[] data) {
        if (version < 2) {
            int start = signatureV1Offset();
            for (int i = start; i < start + Signature.ECDSA_SIGNATURE_SIZE; i++) {
                data[i] = 0;
            }
        }
        return data;
    }

    private int getFlags() {
        int flags = 0;
        int position = 1;
        if (message != null) {
            flags |= position;
        }
        position <<= 1;
        if (encryptedMessage != null) {
            flags |= position;
        }
        position <<= 1;
        if (publicKeyAnnouncement != null) {
            flags |= position;
        }
        position <<= 1;
        if (encryptToSelfMessage != null) {
            flags |= position;
        }
        position <<= 1;
        if (phasing != null) {
            flags |= position;
        }
        position <<= 1;
        if (prunablePlainMessage != null) {
            flags |= position;
        }
        position <<= 1;
        if (prunableEncryptedMessage != null) {
            flags |= position;
        }
        return flags;
    }

    @Override
    /**
     * @deprecated see method with longer parameters list below
     */
    public boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        if (!attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            // all are checked at acceptance height for block duplicates
            if (type.isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return type.isDuplicate(this, duplicates);
    }

    public boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates,
                                         boolean atAcceptanceHeight,
                                         Set<AccountControlType> senderAccountControls,
                                         AccountControlPhasing accountControlPhasing) {
        if (!attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            if (this.isBlockDuplicate(
                this, duplicates, senderAccountControls, accountControlPhasing)) {
                return true;
            }
            // all are checked at acceptance height for block duplicates
            if (type.isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return type.isDuplicate(this, duplicates);
    }

    private boolean isBlockDuplicate(Transaction transaction,
                                     Map<TransactionType, Map<String, Integer>> duplicates,
                                     Set<AccountControlType> senderAccountControls,
                                     AccountControlPhasing accountControlPhasing) {
        return
            senderAccountControls.contains(AccountControlType.PHASING_ONLY)
                && (accountControlPhasing != null && accountControlPhasing.getMaxFees() != 0)
                && transaction.getType() != SET_PHASING_ONLY
                && TransactionType.isDuplicate(SET_PHASING_ONLY,
                Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
    }

    @Override
    public boolean isUnconfirmedDuplicate(Map<TransactionType, Map<String, Integer>> duplicates) {
        return type.isUnconfirmedDuplicate(this, duplicates);
    }

    public static final class BuilderImpl implements Builder {

        private short deadline;
        private byte[] senderPublicKey;
        private long amountATM;
        private long feeATM;
        private TransactionType type;
        private byte version;
        private AbstractAttachment attachment;

        private long recipientId;
        private byte[] referencedTransactionFullHash;
        private Signature signature;
        private MessageAppendix message;
        private EncryptedMessageAppendix encryptedMessage;
        private EncryptToSelfMessageAppendix encryptToSelfMessage;
        private PublicKeyAnnouncementAppendix publicKeyAnnouncement;
        private PhasingAppendix phasing;
        private PrunablePlainMessageAppendix prunablePlainMessage;
        private PrunableEncryptedMessageAppendix prunableEncryptedMessage;
        private long blockId;
        private int height = Integer.MAX_VALUE;
        private long id;
        private long senderId;
        private int timestamp;
        private int blockTimestamp = -1;
        private byte[] fullHash;
        private boolean ecBlockSet = false;
        private int ecBlockHeight;
        private long ecBlockId;
        private short index = -1;
        private long dbId = 0;

        public BuilderImpl(byte version, byte[] senderPublicKey, long amountATM, long feeATM, short deadline,
                           AbstractAttachment attachment, int timestamp) {
            this.version = version;
            this.deadline = deadline;
            this.senderPublicKey = senderPublicKey;
            this.amountATM = amountATM;
            this.feeATM = feeATM;
            this.attachment = attachment;
            this.type = attachment != null ? attachment.getTransactionType() : null;
            if (timestamp < 0) {
                throw new IllegalArgumentException("Timestamp cannot be less than 0");
            }
            this.timestamp = timestamp;
        }

        @Override
        public TransactionImpl build() throws AplException.NotValidException {
            return build(null);
        }

        @Override
        public TransactionImpl build(byte[] keySeed) throws AplException.NotValidException {
            if (!ecBlockSet) {
                EcBlockData ecBlock = lookupAndInjectBlockChain().getECBlock(timestamp);
                this.ecBlockHeight = ecBlock.getHeight();
                this.ecBlockId = ecBlock.getId();
            }
            TransactionImpl transaction = new TransactionImpl(this);
            if (keySeed != null) {
                for (Appendix appendage : transaction.getAppendages()) {
                    if (appendage instanceof Encryptable) {//encrypt attached message
                        ((Encryptable) appendage).encrypt(keySeed);
                    }
                }
            }
            return transaction;
        }

        private static Blockchain lookupAndInjectBlockChain() {
            return CDI.current().select(Blockchain.class).get();
        }

        @Override
        public BuilderImpl recipientId(long recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        @Override
        public BuilderImpl referencedTransactionFullHash(String referencedTransactionFullHash) {
            this.referencedTransactionFullHash = Convert.parseHexString(referencedTransactionFullHash);
            return this;
        }

        @Override
        public BuilderImpl referencedTransactionFullHash(byte[] referencedTransactionFullHash) {
            this.referencedTransactionFullHash = referencedTransactionFullHash;
            return this;
        }

        BuilderImpl appendix(AbstractAttachment attachment) {
            this.attachment = attachment;
            return this;
        }

        @Override
        public BuilderImpl appendix(MessageAppendix message) {
            this.message = message;
            return this;
        }

        @Override
        public BuilderImpl appendix(EncryptedMessageAppendix encryptedMessage) {
            this.encryptedMessage = encryptedMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(EncryptToSelfMessageAppendix encryptToSelfMessage) {
            this.encryptToSelfMessage = encryptToSelfMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(PublicKeyAnnouncementAppendix publicKeyAnnouncement) {
            this.publicKeyAnnouncement = publicKeyAnnouncement;
            return this;
        }

        @Override
        public BuilderImpl appendix(PrunablePlainMessageAppendix prunablePlainMessage) {
            this.prunablePlainMessage = prunablePlainMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(PrunableEncryptedMessageAppendix prunableEncryptedMessage) {
            this.prunableEncryptedMessage = prunableEncryptedMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(PhasingAppendix phasing) {
            this.phasing = phasing;
            return this;
        }

        @Override
        public BuilderImpl timestamp(int timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public BuilderImpl ecBlockHeight(int height) {
            this.ecBlockHeight = height;
            this.ecBlockSet = true;
            return this;
        }

        @Override
        public BuilderImpl dbId(long dbId) {
            this.dbId = dbId;
            return this;
        }

        @Override
        public BuilderImpl ecBlockId(long blockId) {
            this.ecBlockId = blockId;
            this.ecBlockSet = true;
            return this;
        }

        @Override
        public BuilderImpl signature(Signature signature) {
            this.signature = signature;
            return this;
        }

        public BuilderImpl id(long id) {
            this.id = id;
            return this;
        }

        @Override
        public BuilderImpl blockId(long blockId) {
            this.blockId = blockId;
            return this;
        }

        public BuilderImpl height(int height) {
            this.height = height;
            return this;
        }

        public BuilderImpl senderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        public BuilderImpl fullHash(byte[] fullHash) {
            this.fullHash = fullHash;
            return this;
        }

        public BuilderImpl blockTimestamp(int blockTimestamp) {
            this.blockTimestamp = blockTimestamp;
            return this;
        }

        public BuilderImpl index(short index) {
            this.index = index;
            return this;
        }
    }
}
