/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author al
 */
public abstract class UpdateAttachment extends AbstractAttachment {

    final OS os;
    final Arch architecture;
    final DoubleByteArrayTuple url;
    final Version updateVersion;
    final byte[] hash;

    public UpdateAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            os = OS.fromCompatible(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_PLATFORM_LENGTH).trim());
            architecture = Arch.fromCompatible(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_ARCHITECTURE_LENGTH).trim());
            int firstUrlPartLength = buffer.getShort();
            byte[] firstUrlPart = new byte[firstUrlPartLength];
            buffer.get(firstUrlPart);
            int secondUrlPartLength = buffer.getShort();
            byte[] secondUrlPart = new byte[secondUrlPartLength];
            buffer.get(secondUrlPart);
            url = new DoubleByteArrayTuple(firstUrlPart, secondUrlPart);
            updateVersion = new Version(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_VERSION_LENGTH).trim());
            int hashLength = buffer.getShort();
            hash = new byte[hashLength];
            buffer.get(hash);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public UpdateAttachment(JSONObject attachmentData) {
        super(attachmentData);
        // try to get data from 'os' json field
        Optional<OS> optionalOS = OS.fromCompatibleOptional(Convert.nullToEmpty((String) attachmentData.get("os")).trim());
        // otherwise try to get value from 'platform' field
        os = optionalOS.orElseGet(() -> OS.fromCompatible(Convert.nullToEmpty((String) attachmentData.get("platform")).trim()));
        architecture = Arch.fromCompatible(Convert.nullToEmpty((String) attachmentData.get("architecture")).trim());
        Map<?,?> urlJson = (Map<?,?>) attachmentData.get("url");
        byte[] firstUrlPart = Convert.parseHexString(Convert.nullToEmpty(((String) urlJson.get("first")).trim()));
        byte[] secondUrlPart = Convert.parseHexString(Convert.nullToEmpty(((String) urlJson.get("second")).trim()));
        url = new DoubleByteArrayTuple(firstUrlPart, secondUrlPart);
        Object version = attachmentData.get("updateVersion");
        if (version == null) {
            version = attachmentData.get("version");
        }
        this.updateVersion = new Version(Convert.nullToEmpty((String) version).trim());
        hash = Convert.parseHexString(Convert.nullToEmpty((String) attachmentData.get("hash")).trim());
    }

    public UpdateAttachment(OS os, Arch architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
        this.os = os;
        this.architecture = architecture;
        this.url = url;
        this.updateVersion = version;
        this.hash = hash;
    }

    public static UpdateAttachment getAttachment(OS platform, Arch architecture, DoubleByteArrayTuple url, Version version, byte[] hash, byte level) {
        if (level == TransactionTypes.SUBTYPE_UPDATE_CRITICAL) {
            return new CriticalUpdate(platform, architecture, url, version, hash);
        } else if (level == TransactionTypes.SUBTYPE_UPDATE_IMPORTANT) {
            return new ImportantUpdate(platform, architecture, url, version, hash);
        } else if (level == TransactionTypes.SUBTYPE_UPDATE_MINOR) {
            return new MinorUpdate(platform, architecture, url, version, hash);
        }
        return null;
    }

    @Override
    public int getMySize() {
        return 1 + Convert.toBytes(os.getCompatibleName()).length + 1 + Convert.toBytes(architecture.getCompatibleName()).length + 2 + url.getFirst().length + 2 + url.getSecond().length + 1 + Convert.toBytes(updateVersion.toString()).length + 2 + hash.length;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        byte[] platform = Convert.toBytes(this.os.getCompatibleName());
        byte[] architecture = Convert.toBytes(this.architecture.getCompatibleName());
        byte[] version = Convert.toBytes(this.updateVersion.toString());
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
    public void putMyJSON(JSONObject attachment) {
        attachment.put("os", os.getCompatibleName());
        attachment.put("architecture", architecture.getCompatibleName());
        JSONObject urlJson = new JSONObject();
        urlJson.put("first", Convert.toHexString(url.getFirst()));
        urlJson.put("second", Convert.toHexString(url.getSecond()));
        attachment.put("url", urlJson);
        //TODO change to 'updateVersion'
        attachment.put("version", updateVersion.toString());
        attachment.put("hash", Convert.toHexString(hash));
    }

    public OS getOS() {
        return os;
    }

    public Arch getArchitecture() {
        return architecture;
    }

    public DoubleByteArrayTuple getUrl() {
        return url;
    }

    public Version getAppVersion() {
        return updateVersion;
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpdateAttachment)) {
            return false;
        }
        UpdateAttachment that = (UpdateAttachment) o;
        return os == that.os && architecture == that.architecture && Objects.equals(url, that.url) && Objects.equals(updateVersion, that.updateVersion) && Arrays.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(os, architecture, url, updateVersion);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }

}
