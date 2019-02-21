/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class UpdateAttachment extends AbstractAttachment {
    
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
        return 1 + Convert.toBytes(platform.name()).length + 1 + Convert.toBytes(architecture.name()).length + 2 + url.getFirst().length + 2 + url.getSecond().length + 1 + Convert.toBytes(version.toString()).length + 2 + hash.length;
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpdateAttachment)) {
            return false;
        }
        UpdateAttachment that = (UpdateAttachment) o;
        return platform == that.platform && architecture == that.architecture && Objects.equals(url, that.url) && Objects.equals(version, that.version) && Arrays.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(platform, architecture, url, version);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }

    public static UpdateAttachment getAttachment(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash, byte level) {
        if (level == Update.CRITICAL.getSubtype()) {
            return new CriticalUpdate(platform, architecture, url, version, hash);
        } else if (level == Update.IMPORTANT.getSubtype()) {
            return new ImportantUpdate(platform, architecture, url, version, hash);
        } else if (level == Update.MINOR.getSubtype()) {
            return new MinorUpdate(platform, architecture, url, version, hash);
        }
        return null;
    }
    
}
