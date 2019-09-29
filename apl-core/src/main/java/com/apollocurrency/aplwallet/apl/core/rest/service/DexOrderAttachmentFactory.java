package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOrderAttachmentFactory {

    public static DexOrderAttachment build(ByteBuffer buffer) throws AplException.NotValidException {
        byte version = buffer.get();

        switch (version)  {
            case 1 :
                return new DexOrderAttachment(buffer);
            case 2 :
                return new DexOrderAttachmentV2(buffer);
            default:
                throw new UnsupportedOperationException("Version: " + version + ", not supported.");
        }
    }

    public static DexOrderAttachment parse(JSONObject attachmentData) {
        if (Appendix.hasAppendix( "DexOrder", attachmentData)) {
            return new DexOrderAttachment(attachmentData);
        } else if (Appendix.hasAppendix("DexOrder_v2", attachmentData)){
            return new DexOrderAttachmentV2(attachmentData);
        }
        return null;
    }
}
