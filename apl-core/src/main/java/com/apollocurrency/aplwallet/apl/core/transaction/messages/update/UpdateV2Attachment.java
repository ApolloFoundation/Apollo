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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
@EqualsAndHashCode(callSuper = false)
@ToString
@Getter
public class UpdateV2Attachment extends AbstractAttachment {
    private final String manifestUrl;
    private final Level updateLevel;
    private final Platform platform;
    private final Architecture architecture;
    private final Version releaseVersion;

    public UpdateV2Attachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.manifestUrl = Convert.readString(buffer, buffer.getShort(), Short.MAX_VALUE);
            this.updateLevel = Level.from(buffer.get());
            this.platform = Platform.from(buffer.get());
            this.architecture = Architecture.from(buffer.get());
            this.releaseVersion = new Version(buffer.get(), buffer.getShort(), buffer.getShort());
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public UpdateV2Attachment(JSONObject attachmentData) {
        super(attachmentData);
        manifestUrl =  Convert.nullToEmpty((String) attachmentData.get("manifestUrl"));
        updateLevel = Level.from((int)Convert.parseLong(attachmentData.get("level")));
        platform = Platform.from((int)Convert.parseLong(attachmentData.get("platform")));
        architecture = Architecture.from((int)Convert.parseLong(attachmentData.get("architecture")));
        releaseVersion = new Version((String)(attachmentData.get("version")));

    }


    public UpdateV2Attachment(String manifestUrl, Level updateLevel, Platform platform, Architecture architecture, Version version) {
        this.manifestUrl = manifestUrl;
        this.updateLevel = updateLevel;
        this.platform = platform;
        this.architecture = architecture;
        this.releaseVersion = version;
    }

    @Override
    public int getMySize() {
        return 2 + manifestUrl.getBytes().length + 1 + 1 + 1 + 5;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        byte[] manifestUrlBytes = manifestUrl.getBytes();
        buffer.putShort((short) manifestUrlBytes.length);
        buffer.put(manifestUrlBytes);
        buffer.put(updateLevel.code);
        buffer.put(platform.code);
        buffer.put(architecture.code);
        buffer.put((byte) releaseVersion.getMajorVersion());
        buffer.putShort((short) releaseVersion.getIntermediateVersion());
        buffer.putShort((short) releaseVersion.getMinorVersion());
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("manifestUrl", manifestUrl);
        attachment.put("level", updateLevel.code);
        attachment.put("platform", platform.code);
        attachment.put("architecture", architecture.code);
        attachment.put("version", releaseVersion.toString());
    }

    @Override
    public TransactionType getTransactionType() {
        return Update.UPDATE_V2;
    }
}
