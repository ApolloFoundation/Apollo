package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachmentV2;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOfferAttachmentFactory {

    public static DexOfferAttachment build(ByteBuffer buffer) throws AplException.NotValidException {
        byte version = buffer.get();

        switch (version)  {
            case 1 :
                return new DexOfferAttachment(buffer);
            case 2 :
                return new DexOfferAttachmentV2(buffer);
            default:
                return null;
        }
    }

    public static DexOfferAttachment parse(JSONObject attachmentData) {
        if (Appendix.hasAppendix( "DexOrder", attachmentData)) {
            return new DexOfferAttachment(attachmentData);
        } else if (Appendix.hasAppendix("DexOrder_v2", attachmentData)){
            return new DexOfferAttachmentV2(attachmentData);
        }
        return null;
    }
}
