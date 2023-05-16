/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.update;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.ImportantUpdate;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class ImportantUpdateTransactionType extends UpdateTransactionType {

    @Inject
    public ImportantUpdateTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public Level getLevel() {
        return Level.IMPORTANT;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.IMPORTANT_UPDATE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.UPDATE_IMPORTANT;
    }

    @Override
    public String getName() {
        return "ImportantUpdate";
    }

    @Override
    public ImportantUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ImportantUpdate(buffer);
    }

    @Override
    public ImportantUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ImportantUpdate(attachmentData);
    }
}
