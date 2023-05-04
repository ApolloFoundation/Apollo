/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.TxErrorHash;
import lombok.Getter;

import java.util.List;

/**
 * Exception, thrown when declared and signed block transaction error statuses are different from the obtained
 * by the block transactions execution
 * @author Andrii Boiarskyi
 * @see AplBlockException
 * @see Block#checkFailedTxsExecution()
 * @since 1.48.4
 */
public class AplBlockTxErrorResultsMismatchException extends AplBlockException {
    @Getter
    private final List<TxErrorHash> calculatedErrorHashes;

    public AplBlockTxErrorResultsMismatchException(Block block, List<TxErrorHash> calculatedErrorHashes) {
        super("Tx errors after execution: " + calculatedErrorHashes + " are not the same as declared: " + block.getTxErrorHashes(), block);
        this.calculatedErrorHashes = calculatedErrorHashes;
    }
}
