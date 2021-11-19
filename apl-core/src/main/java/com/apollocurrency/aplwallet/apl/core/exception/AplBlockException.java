/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import lombok.Getter;

/**
 * <p>Represents any block-related exception, including validation, serialization/deserialization, push/rollback/generate
 * failures.
 * </p>
 * <p>Should be used as a base exception for derived classes, which are responsible for specific error cases</p>
 * <p><b>NOTE: </b> Class is intended to fully replace
 * {@link com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor.BlockNotAcceptedException} and
 * its hierarchy</p>
 * @author Andrii Boiarskyi
 * @see AplCoreLogicException
 * @see com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor.BlockNotAcceptedException
 * @since 1.48.4
 */
@Getter
public class AplBlockException extends AplCoreLogicException {
    private final Block block;

    public AplBlockException(String message, Block block) {
        super(constructErrorMessage(message, block));
        this.block = block;
    }

    public AplBlockException(String message, Throwable cause, Block block) {
        super(constructErrorMessage(message, block), cause);
        this.block = block;
    }

    private static String constructErrorMessage(String message, Block block) {
        return message + ", block: " + block.getStringId();
    }

}
