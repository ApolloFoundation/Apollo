/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.update;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class UpdateV2TransactionType extends UpdateTransactionType {
    @Inject
    public UpdateV2TransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public Level getLevel() {
        throw new UnsupportedOperationException("Level is not defined for UpdateV2 statically");
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.NotValidException {
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        UpdateV2Attachment attachment = (UpdateV2Attachment) transaction.getAttachment();
        Version version = attachment.getReleaseVersion();
        if (version.getMinorVersion() > Short.MAX_VALUE || version.getIntermediateVersion() > Short.MAX_VALUE || version.getMajorVersion() > Short.MAX_VALUE) {
            throw new AplException.NotValidException("Update version is too big! " + version);
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        super.applyAttachment(transaction, senderAccount, recipientAccount);
        UpdateV2Attachment attachment = (UpdateV2Attachment) transaction.getAttachment();
        if (attachment.getUpdateLevel() == Level.CRITICAL && attachment.getPlatforms().contains(PlatformSpec.current())) {
            // TODO send message to supervisor
        }
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.UPDATE_V2;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.UPDATE_V2;
    }

    @Override
    public String getName() {
        return "UpdateV2";
    }

    @Override
    public UpdateV2Attachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new UpdateV2Attachment(buffer);
    }

    @Override
    public UpdateV2Attachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new UpdateV2Attachment(attachmentData);
    }
}
