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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.HoldingType;
import com.apollocurrency.aplwallet.apl.core.app.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.app.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.app.transaction.AccountControl;
import com.apollocurrency.aplwallet.apl.core.app.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Data;
import com.apollocurrency.aplwallet.apl.core.app.transaction.DigitalGoods;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Update;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Platform;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface Attachment extends Appendix {

    public TransactionType getTransactionType();



    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Payment.ORDINARY;
        }

    };

    EmptyAttachment PRIVATE_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Payment.PRIVATE;
        }

    };



    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Messaging.ARBITRARY_MESSAGE;
        }

    };
























    abstract class TaggedDataAttachment extends AbstractAttachment implements Prunable {

        final String name;
        final String description;
        final String tags;
        final String type;
        final String channel;
        final boolean isText;
        final String filename;
        final byte[] data;
        private volatile TaggedData taggedData;

        public TaggedDataAttachment(ByteBuffer buffer) {
            super(buffer);
            this.name = null;
            this.description = null;
            this.tags = null;
            this.type = null;
            this.channel = null;
            this.isText = false;
            this.filename = null;
            this.data = null;
        }

        public TaggedDataAttachment(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON != null) {
                this.name = (String) attachmentData.get("name");
                this.description = (String) attachmentData.get("description");
                this.tags = (String) attachmentData.get("tags");
                this.type = (String) attachmentData.get("type");
                this.channel = Convert.nullToEmpty((String) attachmentData.get("channel"));
                this.isText = Boolean.TRUE.equals(attachmentData.get("isText"));
                this.data = isText ? Convert.toBytes(dataJSON) : Convert.parseHexString(dataJSON);
                this.filename = (String) attachmentData.get("filename");
            } else {
                this.name = null;
                this.description = null;
                this.tags = null;
                this.type = null;
                this.channel = null;
                this.isText = false;
                this.filename = null;
                this.data = null;
            }

        }

        public TaggedDataAttachment(String name, String description, String tags, String type, String channel, boolean isText, String filename, byte[] data) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.type = type;
            this.channel = channel;
            this.isText = isText;
            this.data = data;
            this.filename = filename;
        }

        @Override
        final int getMyFullSize() {
            if (getData() == null) {
                return 0;
            }
            return Convert.toBytes(getName()).length + Convert.toBytes(getDescription()).length + Convert.toBytes(getType()).length
                    + Convert.toBytes(getChannel()).length + Convert.toBytes(getTags()).length + Convert.toBytes(getFilename()).length + getData().length;
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            if (taggedData != null) {
                attachment.put("name", taggedData.getName());
                attachment.put("description", taggedData.getDescription());
                attachment.put("tags", taggedData.getTags());
                attachment.put("type", taggedData.getType());
                attachment.put("channel", taggedData.getChannel());
                attachment.put("isText", taggedData.isText());
                attachment.put("filename", taggedData.getFilename());
                attachment.put("data", taggedData.isText() ? Convert.toString(taggedData.getData()) : Convert.toHexString(taggedData.getData()));
            } else if (data != null) {
                attachment.put("name", name);
                attachment.put("description", description);
                attachment.put("tags", tags);
                attachment.put("type", type);
                attachment.put("channel", channel);
                attachment.put("isText", isText);
                attachment.put("filename", filename);
                attachment.put("data", isText ? Convert.toString(data) : Convert.toHexString(data));
            }
        }

        @Override
        public byte[] getHash() {
            if (data == null) {
                return null;
            }
            MessageDigest digest = Crypto.sha256();
            digest.update(Convert.toBytes(name));
            digest.update(Convert.toBytes(description));
            digest.update(Convert.toBytes(tags));
            digest.update(Convert.toBytes(type));
            digest.update(Convert.toBytes(channel));
            digest.update((byte)(isText ? 1 : 0));
            digest.update(Convert.toBytes(filename));
            digest.update(data);
            return digest.digest();
        }

        public final String getName() {
            if (taggedData != null) {
                return taggedData.getName();
            }
            return name;
        }

        public final String getDescription() {
            if (taggedData != null) {
                return taggedData.getDescription();
            }
            return description;
        }

        public final String getTags() {
            if (taggedData != null) {
                return taggedData.getTags();
            }
            return tags;
        }

        public final String getType() {
            if (taggedData != null) {
                return taggedData.getType();
            }
            return type;
        }

        public final String getChannel() {
            if (taggedData != null) {
                return taggedData.getChannel();
            }
            return channel;
        }

        public final boolean isText() {
            if (taggedData != null) {
                return taggedData.isText();
            }
            return isText;
        }

        public final String getFilename() {
            if (taggedData != null) {
                return taggedData.getFilename();
            }
            return filename;
        }

        public final byte[] getData() {
            if (taggedData != null) {
                return taggedData.getData();
            }
            return data;
        }

        @Override
        public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && taggedData == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                taggedData = TaggedData.getData(getTaggedDataId(transaction));
            }
        }

        @Override
        public boolean hasPrunableData() {
            return (taggedData != null || data != null);
        }

        abstract long getTaggedDataId(Transaction transaction);

    }

    final class TaggedDataUpload extends TaggedDataAttachment {

        public static TaggedDataUpload parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(Data.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataUpload(attachmentData);
        }

        final byte[] hash;

        public TaggedDataUpload(ByteBuffer buffer) {
            super(buffer);
            this.hash = new byte[32];
            buffer.get(hash);
        }

        public TaggedDataUpload(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON == null) {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String)attachmentData.get("hash")));
            } else {
                this.hash = null;
            }
        }

        public TaggedDataUpload(String name, String description, String tags, String type, String channel, boolean isText,
                                String filename, byte[] data) throws AplException.NotValidException {
            super(name, description, tags, type, channel, isText, filename, data);
            this.hash = null;
            if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
                throw new AplException.NotValidException("Data is not UTF-8 text");
            }
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put(getHash());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return Data.TAGGED_DATA_UPLOAD;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            }
            return super.getHash();
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return transaction.getId();
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            TaggedData.restore(transaction, this, blockTimestamp, height);
        }

    }

    final class TaggedDataExtend extends TaggedDataAttachment {
        private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

        public static TaggedDataExtend parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataExtend(attachmentData);
        }

        private volatile byte[] hash;
        final long taggedDataId;
        final boolean jsonIsPruned;

        public TaggedDataExtend(ByteBuffer buffer) {
            super(buffer);
            this.taggedDataId = buffer.getLong();
            this.jsonIsPruned = false;
        }

        public TaggedDataExtend(JSONObject attachmentData) {
            super(attachmentData);
            this.taggedDataId = Convert.parseUnsignedLong((String)attachmentData.get("taggedData"));
            this.jsonIsPruned = attachmentData.get("data") == null;
        }

        public TaggedDataExtend(TaggedData taggedData) {
            super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(),
                    taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
            this.taggedDataId = taggedData.getId();
            this.jsonIsPruned = false;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(taggedDataId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("taggedData", Long.toUnsignedString(taggedDataId));
        }

        @Override
        public TransactionType getTransactionType() {
            return Data.TAGGED_DATA_EXTEND;
        }

        public long getTaggedDataId() {
            return taggedDataId;
        }

        @Override
        public byte[] getHash() {
            if (hash == null) {
                hash = super.getHash();
            }
            if (hash == null) {
                TaggedDataUpload taggedDataUpload = (TaggedDataUpload) blockchain.getTransaction(taggedDataId).getAttachment();
                hash = taggedDataUpload.getHash();
            }
            return hash;
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return taggedDataId;
        }

        public boolean jsonIsPruned() {
            return jsonIsPruned;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        }

    }

    final class SetPhasingOnly extends AbstractAttachment {

        private final PhasingParams phasingParams;
        final long maxFees;
        final short minDuration;
        final short maxDuration;

        public SetPhasingOnly(PhasingParams params, long maxFees, short minDuration, short maxDuration) {
            phasingParams = params;
            this.maxFees = maxFees;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        public SetPhasingOnly(ByteBuffer buffer) {
            super(buffer);
            phasingParams = new PhasingParams(buffer);
            maxFees = buffer.getLong();
            minDuration = buffer.getShort();
            maxDuration = buffer.getShort();
        }

        public SetPhasingOnly(JSONObject attachmentData) {
            super(attachmentData);
            JSONObject phasingControlParams = (JSONObject) attachmentData.get("phasingControlParams");
            phasingParams = new PhasingParams(phasingControlParams);
            maxFees = Convert.parseLong(attachmentData.get("controlMaxFees"));
            minDuration = ((Long)attachmentData.get("controlMinDuration")).shortValue();
            maxDuration = ((Long)attachmentData.get("controlMaxDuration")).shortValue();
        }

        @Override
        public TransactionType getTransactionType() {
            return AccountControl.SET_PHASING_ONLY;
        }

        @Override
        int getMySize() {
            return phasingParams.getMySize() + 8 + 2 + 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            phasingParams.putMyBytes(buffer);
            buffer.putLong(maxFees);
            buffer.putShort(minDuration);
            buffer.putShort(maxDuration);
        }

        @Override
        void putMyJSON(JSONObject json) {
            JSONObject phasingControlParams = new JSONObject();
            phasingParams.putMyJSON(phasingControlParams);
            json.put("phasingControlParams", phasingControlParams);
            json.put("controlMaxFees", maxFees);
            json.put("controlMinDuration", minDuration);
            json.put("controlMaxDuration", maxDuration);
        }

        public PhasingParams getPhasingParams() {
            return phasingParams;
        }

        public long getMaxFees() {
            return maxFees;
        }

        public short getMinDuration() {
            return minDuration;
        }

        public short getMaxDuration() {
            return maxDuration;
        }

    }

    abstract class UpdateAttachment extends AbstractAttachment {

        final Platform platform;
        final Architecture architecture;
        final DoubleByteArrayTuple url;
        final Version version;
        final byte[] hash;

        public UpdateAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            try {
                platform = Platform.valueOf(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_PLATFORM_LENGTH).trim());
                architecture = Architecture.valueOf(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_ARCHITECTURE_LENGTH).trim());
                int firstUrlPartLength = buffer.getShort();
                byte[] firstUrlPart = new byte[firstUrlPartLength];
                buffer.get(firstUrlPart);
                int secondUrlPartLength = buffer.getShort();
                byte[] secondUrlPart = new byte[secondUrlPartLength];
                buffer.get(secondUrlPart);
                url = new DoubleByteArrayTuple(firstUrlPart, secondUrlPart);
                version = new Version(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_VERSION_LENGTH).trim());
                int hashLength = buffer.getShort();
                hash = new byte[hashLength];
                buffer.get(hash);
            } catch (NotValidException ex) {
                throw new AplException.NotValidException(ex.getMessage());
            }
        }

        public UpdateAttachment(JSONObject attachmentData) {
            super(attachmentData);
            platform = Platform.valueOf(Convert.nullToEmpty((String) attachmentData.get("platform")).trim());
            architecture = Architecture.valueOf(Convert.nullToEmpty((String) attachmentData.get("architecture")).trim());
            JSONObject urlJson = (JSONObject) attachmentData.get("url");
            byte[] firstUrlPart = Convert.parseHexString(Convert.nullToEmpty(((String) urlJson.get("first")).trim()));
            byte[] secondUrlPart = Convert.parseHexString(Convert.nullToEmpty(((String) urlJson.get("second")).trim()));
            url = new DoubleByteArrayTuple(firstUrlPart, secondUrlPart);
            version = new Version(Convert.nullToEmpty((String) attachmentData.get("version")).trim());
            hash = Convert.parseHexString(Convert.nullToEmpty((String) attachmentData.get("hash")).trim());
        }

        public UpdateAttachment(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            this.platform = platform;
            this.architecture = architecture;
            this.url = url;
            this.version = version;
            this.hash = hash;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(platform.name()).length + 1 + Convert.toBytes(architecture.name()).length
                    + 2 + url.getFirst().length + 2 + url.getSecond().length + 1 + Convert.toBytes(version.toString()).length + 2+ hash.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] platform = Convert.toBytes(this.platform.toString());
            byte[] architecture = Convert.toBytes(this.architecture.toString());
            byte[] version = Convert.toBytes(this.version.toString());
            buffer.put((byte) platform.length);
            buffer.put(platform);
            buffer.put((byte) architecture.length);
            buffer.put(architecture);
            buffer.putShort((short) url.getFirst().length);
            buffer.put(url.getFirst());
            buffer.putShort((short) url.getSecond().length);
            buffer.put(url.getSecond());
            buffer.put((byte) version.length);
            buffer.put(version);
            buffer.putShort((short) hash.length);
            buffer.put(hash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("platform", platform.toString());
            attachment.put("architecture", architecture.toString());
            JSONObject urlJson = new JSONObject();
            urlJson.put("first", Convert.toHexString(url.getFirst()));
            urlJson.put("second", Convert.toHexString(url.getSecond()));
            attachment.put("url", urlJson);
            attachment.put("version", version.toString());
            attachment.put("hash", Convert.toHexString(hash));
        }

        public Platform getPlatform() {
            return platform;
        }

        public Architecture getArchitecture() {
            return architecture;
        }

        public DoubleByteArrayTuple getUrl() {
            return url;
        }

        public Version getAppVersion() {
            return version;
        }

        public byte[] getHash() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UpdateAttachment)) return false;
            UpdateAttachment that = (UpdateAttachment) o;
            return platform == that.platform &&
                    architecture == that.architecture &&
                    Objects.equals(url, that.url) &&
                    Objects.equals(version, that.version) &&
                    Arrays.equals(hash, that.hash);
        }

        @Override
        public int hashCode() {

            int result = Objects.hash(platform, architecture, url, version);
            result = 31 * result + Arrays.hashCode(hash);
            return result;
        }

        public static Attachment.UpdateAttachment getAttachment(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[]
                hash, byte level) {
            if (level == Update.CRITICAL.getSubtype()) {
                return new Attachment.CriticalUpdate(platform, architecture, url, version, hash);
            } else if (level == Update.IMPORTANT.getSubtype()) {
                return new Attachment.ImportantUpdate(platform, architecture, url, version, hash);
            } else if (level == Update.MINOR.getSubtype()) {
                return new Attachment.MinorUpdate(platform, architecture, url, version, hash);
            }
            return null;
        }
    }

    final class CriticalUpdate extends UpdateAttachment {
        public CriticalUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        public CriticalUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public CriticalUpdate(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public TransactionType getTransactionType() {
            return Update.CRITICAL;
        }
    }

    final class ImportantUpdate extends UpdateAttachment {
        public ImportantUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        public ImportantUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ImportantUpdate(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public TransactionType getTransactionType() {
            return Update.IMPORTANT;
        }
    }

    final class MinorUpdate extends UpdateAttachment {
        public MinorUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        public MinorUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MinorUpdate(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public TransactionType getTransactionType() {
            return Update.MINOR;
        }
    }

}
