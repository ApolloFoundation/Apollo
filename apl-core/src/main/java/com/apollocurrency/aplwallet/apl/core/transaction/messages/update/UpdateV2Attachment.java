package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class UpdateV2Attachment extends AbstractAttachment {
    private final String manifestUrl;
    private final Level updateLevel;

    public UpdateV2Attachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.manifestUrl = Convert.readString(buffer, buffer.getShort(), Short.MAX_VALUE);
            this.updateLevel = Level.from(buffer.get());
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public UpdateV2Attachment(JSONObject attachmentData) {
        super(attachmentData);
        manifestUrl =  Convert.nullToEmpty((String) attachmentData.get("manifestUrl"));
        updateLevel = Level.from((int)Convert.parseLong(attachmentData.get("level")));
    }

    public UpdateV2Attachment(String manifestUrl, Level level) {
        this.manifestUrl = manifestUrl;
        this.updateLevel = level;
    }

    @Override
    public int getMySize() {
        return 2 + manifestUrl.getBytes().length + 1;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        byte[] manifestUrlBytes = manifestUrl.getBytes();
        buffer.putShort((short) manifestUrlBytes.length);
        buffer.put(manifestUrlBytes);
        buffer.put(updateLevel.getCode());
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("manifestUrl", manifestUrl);
        attachment.put("level", updateLevel.getCode());
    }

    @Override
    public TransactionType getTransactionType() {
        return Update.UPDATE_V2;
    }
}
