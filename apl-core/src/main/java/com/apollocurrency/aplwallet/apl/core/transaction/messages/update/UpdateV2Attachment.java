package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@ToString
@Getter
public class UpdateV2Attachment extends AbstractAttachment {
    private final String manifestUrl;
    private final Level updateLevel;
    private final Set<PlatformPair> platforms = new HashSet<>();
    private final Version releaseVersion;
    private final String cn;
    private final BigInteger serialNumber;
    private final byte[] signature;

    public UpdateV2Attachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.manifestUrl = Convert.readString(buffer, buffer.getShort(), 2048);
            this.updateLevel = Level.from(buffer.get());
            byte platformsLength = buffer.get();
            for (int i = 0; i < platformsLength; i++) {
                this.platforms.add(new PlatformPair(Platform.from(buffer.get()), Architecture.from(buffer.get())));
            }
            this.releaseVersion = new Version(buffer.getShort(), buffer.getShort(), buffer.getShort());
            this.cn = Convert.readString(buffer, buffer.getShort(), 2048);
            byte[] serialNumberBytes = new byte[buffer.getShort()];
            buffer.get(serialNumberBytes);
            this.serialNumber = new BigInteger(serialNumberBytes);
            short sigLength = buffer.getShort();
            this.signature = new byte[sigLength];
            buffer.get(this.signature);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public UpdateV2Attachment(JSONObject attachmentData) {
        super(attachmentData);
        this.manifestUrl =  Convert.nullToEmpty((String) attachmentData.get("manifestUrl"));
        this.updateLevel = Level.from((int)Convert.parseLong(attachmentData.get("level")));
        JSONArray platforms = (JSONArray) attachmentData.get("platforms");
        for (Object platformObj : platforms) {
            JSONObject platformJsonObj = (JSONObject) platformObj;
            PlatformPair platformPair = new PlatformPair(Platform.from((int) platformJsonObj.get("platform")), Architecture.from((int) platformJsonObj.get("architecture")));
            this.platforms.add(platformPair);
        }
        this.cn = (String) attachmentData.get("cn");
        this.signature = Convert.parseHexString((String) attachmentData.get("signature"));
        this.serialNumber = new BigInteger((String) attachmentData.get("serialNumber"));
        this.releaseVersion = new Version((String)(attachmentData.get("version")));

    }


    public UpdateV2Attachment(String manifestUrl, Level updateLevel, Version releaseVersion, String cn, BigInteger serialNumber, byte[] signature) {
        this.manifestUrl = manifestUrl;
        this.updateLevel = updateLevel;
        this.releaseVersion = releaseVersion;
        this.cn = cn;
        this.serialNumber = serialNumber;
        this.signature = signature;
    }

    @Override
    public int getMySize() {
        return 2 + manifestUrl.getBytes().length + 1 + 1 + 2 * this.platforms.size() + 6 + 2 + this.cn.getBytes().length + 2 + this.serialNumber.toByteArray().length + 2 + signature.length;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        byte[] manifestUrlBytes = manifestUrl.getBytes();
        buffer.putShort((short) manifestUrlBytes.length);
        buffer.put(manifestUrlBytes);
        buffer.put(updateLevel.code);
        buffer.put((byte) platforms.size());
        for (PlatformPair platformPair : platforms) {
            buffer.put(platformPair.platform.code);
            buffer.put(platformPair.architecture.code);
        }
        buffer.putShort((short) releaseVersion.getMajorVersion());
        buffer.putShort((short) releaseVersion.getIntermediateVersion());
        buffer.putShort((short) releaseVersion.getMinorVersion());
        buffer.putShort((short) cn.length());
        buffer.put(cn.getBytes());
        byte[] snBytes = serialNumber.toByteArray();
        buffer.putShort((short) snBytes.length);
        buffer.put(snBytes);
        buffer.putShort((short) signature.length);
        buffer.put(signature);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("manifestUrl", manifestUrl);
        attachment.put("level", updateLevel.code);
        JSONArray platformArray = new JSONArray();
        for (PlatformPair platformPair : this.platforms) {
            platformArray.put(new JSONObject(Map.of("platform", platformPair.platform.code, "architecture", platformPair.architecture.code)));
        }
        attachment.put("platforms", platformArray);
        attachment.put("version", releaseVersion.toString());
        attachment.put("cn", this.cn);
        attachment.put("serialNumber", this.serialNumber.toString());
        attachment.put("signature", Convert.toHexString(this.signature));
    }

    @Override
    public TransactionType getTransactionType() {
        return Update.UPDATE_V2;
    }

    @Data
    public static class PlatformPair {
        private final Platform platform;
        private final Architecture architecture;
    }
}
