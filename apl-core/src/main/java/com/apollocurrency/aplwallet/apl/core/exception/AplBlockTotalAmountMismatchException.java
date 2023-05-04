/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import lombok.Getter;

/**
 * Block validation exception, specifying case, where calculated total block amount by successfully executed transactions
 * amounts summarizing (failed txs amount are skipped) doesn't match to the declared and signed block total amount
 * @author Andrii Boiarskyi
 * @see AplBlockException
 * @see AplBlockPayloadSizeMismatchException
 * @since 1.48.4
 */
public class AplBlockTotalAmountMismatchException extends AplBlockException {
    @Getter
    private final long calculatedAmount;

    public AplBlockTotalAmountMismatchException(Block block, long calculatedAmount) {
        super("Declared block total amount " + block.getTotalAmountATM() + " doesn't match transaction totals " + calculatedAmount, block);
        this.calculatedAmount = calculatedAmount;
    }


}
