/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Hashed transaction failed status, signed by the block generator
 * @author Andrii Boiarskyi
 * @see Block
 * @since 1.48.4
 */
@Getter
@EqualsAndHashCode
public class TxErrorHash {
    private final long id;
    private final byte[] errorHash;
    private final String error;

    public TxErrorHash(long id, String error) {
        this.id = id;
        this.error = error;
        this.errorHash = Crypto.sha256().digest(error.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "TxErrorHash{" +
            "id=" + Long.toUnsignedString(id) +
            ", errorHash=" + Convert.toHexString(errorHash) +
            ", error=" + error +
            '}';
    }
}
