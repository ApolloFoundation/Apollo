/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class EmptyAttachment extends AbstractAttachment {
    
    public EmptyAttachment() {
        super(0);
    }

    @Override
    public final int getMySize() {
        return 0;
    }

    @Override
    public final void putMyBytes(ByteBuffer buffer) {
    }

    @Override
    public final void putMyJSON(JSONObject json) {
    }

    @Override
    public boolean verifyVersion() {
        return getVersion() == 0;
    }
    
}
