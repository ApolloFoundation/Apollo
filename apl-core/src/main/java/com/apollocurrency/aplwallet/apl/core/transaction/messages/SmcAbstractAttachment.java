/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class SmcAbstractAttachment extends AbstractAttachment {
    public SmcAbstractAttachment(int version) {
        super(version);
    }
}
