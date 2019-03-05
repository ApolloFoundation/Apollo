/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MinorUpdate extends UpdateAttachment {
    
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
