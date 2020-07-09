/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.update;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.CriticalUpdate;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class CriticalUpdateTransactiionType extends UpdateTransactionType {
    @Inject
    public CriticalUpdateTransactiionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public Level getLevel() {
        return Level.CRITICAL;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CRITICAL_UPDATE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.UPDATE_CRITICAL;
    }

    @Override
    public String getName() {
        return "CriticalUpdate";
    }

    @Override
    public CriticalUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new CriticalUpdate(buffer);
    }

    @Override
    public CriticalUpdate parseAttachment(JSONObject attachmentData) {
        return new CriticalUpdate(attachmentData);
    }
}
