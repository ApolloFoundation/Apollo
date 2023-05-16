/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class MinorUpdate extends UpdateAttachment {

    public MinorUpdate(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public MinorUpdate(JSONObject attachmentData) {
        super(attachmentData);
    }

    public MinorUpdate(OS OS, Arch architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
        super(OS, architecture, url, version, hash);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.MINOR_UPDATE;
    }

}
