/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.update;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.MinorUpdate;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MinorUpdateTransactionType extends UpdateTransactionType {
    @Inject
    public MinorUpdateTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public Level getLevel() {
        return Level.MINOR;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MINOR_UPDATE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.UPDATE_MINOR;
    }

    @Override
    public String getName() {
        return "MinorUpdate";
    }

    @Override
    public MinorUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MinorUpdate(buffer);
    }

    @Override
    public MinorUpdate parseAttachment(JSONObject attachmentData) {
        return new MinorUpdate(attachmentData);
    }
}
