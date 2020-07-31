/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhasingAppendixV2Validator implements AppendixValidator<PhasingAppendixV2> {
    private final PhasingAppendixValidator phasingAppendixValidator;
    private final Blockchain blockchain;
    private final TimeService timeService;

    @Inject
    public PhasingAppendixV2Validator(PhasingAppendixValidator phasingAppendixValidator, Blockchain blockchain, TimeService timeService) {
        this.phasingAppendixValidator = phasingAppendixValidator;
        this.blockchain = blockchain;
        this.timeService = timeService;
    }

    public void validateFinishHeightAndTime(Integer height, Integer time, PhasingAppendix phasingAppendix) throws AplException.NotCurrentlyValidException {
        int finishHeight = phasingAppendix.getFinishHeight();
        if ((finishHeight != -1 && time != -1) || (finishHeight == -1 && time == -1)) {
            throw new AplException.NotCurrentlyValidException("Only one parameter should be filled 'phasingFinishHeight or phasingFinishTime'");
        }

        Block lastBlock = blockchain.getLastBlock();
        int lastBlockHeight = lastBlock.getHeight();
        int currentTime = timeService.getEpochTime();

        if (time == -1 &&
            (finishHeight <= lastBlockHeight + (phasingAppendix.getParams().getVoteWeighting().acceptsVotes() ? 2 : 1) ||
                finishHeight >= lastBlockHeight + Constants.MAX_PHASING_DURATION)) {
            throw new AplException.NotCurrentlyValidException("Invalid finish height " + height);
        }


        if (finishHeight == -1 && time >= currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC) {
            throw new AplException.NotCurrentlyValidException("Invalid finish time " + time);
        }

    }

    @Override
    public void validate(Transaction transaction, PhasingAppendixV2 appendix, int validationHeight) throws AplException.ValidationException {
        phasingAppendixValidator.generalValidation(transaction, appendix);

        validateFinishHeightAndTime(validationHeight, appendix.getFinishTime(), appendix);
    }

    @Override
    public void validateAtFinish(Transaction transaction, PhasingAppendixV2 appendix, int blockHeight) throws AplException.ValidationException {
        phasingAppendixValidator.validateAtFinish(transaction, appendix, blockHeight);
    }
}
