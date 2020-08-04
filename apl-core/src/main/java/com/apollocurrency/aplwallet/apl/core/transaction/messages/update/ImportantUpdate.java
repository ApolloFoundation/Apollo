/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class ImportantUpdate extends UpdateAttachment {

    public ImportantUpdate(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public ImportantUpdate(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ImportantUpdate(OS OS, Arch architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
        super(OS, architecture, url, version, hash);
    }

    @Override
    public TransactionType getTransactionType() {
        return Update.IMPORTANT;
    }

}
