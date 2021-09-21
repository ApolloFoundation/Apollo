/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import lombok.Getter;

/**
 * Exception, specifying an exceptional case, where calculated block payload size using executed transactions (including
 * failed txs state) doesn't match block declared and signed payload size
 * @author Andrii Boiarskyi
 * @see AplBlockException
 * @see AplBlockTotalAmountMismatchException
 * @since 1.48.4
 */
public class AplBlockPayloadSizeMismatchException extends AplBlockException {
    @Getter
    private final int calculatedPayloadLength;

    public AplBlockPayloadSizeMismatchException(Block block, int calculatedPayloadLength) {
        super("Transaction payload length " + calculatedPayloadLength + " does not match block payload length "
            + block.getPayloadLength(), block);
        this.calculatedPayloadLength = calculatedPayloadLength;
    }
}
