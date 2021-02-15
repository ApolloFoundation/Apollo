/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.prunable.impl;

import com.apollocurrency.aplwallet.apl.core.dao.prunable.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class PrunableMessageServiceImpl implements PrunableMessageService {
    private PrunableMessageTable table;
    private Blockchain blockchain;
    private AccountPublicKeyService accountPublicKeyService;

    @Inject
    public PrunableMessageServiceImpl(PrunableMessageTable table, Blockchain blockchain, AccountPublicKeyService accountPublicKeyService) {
        this.table = table;
        this.blockchain = blockchain;
        this.accountPublicKeyService = accountPublicKeyService;
    }

    @Override
    public int getCount() {
        return table.getCount();
    }

    @Override
    public List<PrunableMessage> getAll(int from, int to) {
        return CollectionUtil.toList(table.getAll(from, to));
    }

    @Override
    public PrunableMessage get(long transactionId) {
        return table.get(transactionId);
    }

    @Override
    public List<PrunableMessage> getAll(long accountId, int from, int to) {
        return table.getPrunableMessages(accountId, from, to);
    }

    @Override
    public List<PrunableMessage> getAll(long accountId, long otherAccountId, int from, int to) {
        return table.getPrunableMessages(accountId, otherAccountId, from, to);
    }

    @Override
    public byte[] decrypt(PrunableMessage message, String secretPhrase) {
        return decryptUsingKeySeed(message, Crypto.getKeySeed(secretPhrase));
    }

    @Override
    public byte[] decryptUsingSharedKey(PrunableMessage message, byte[] sharedKey) {
        if (message.getEncryptedData() == null) {
            return null;
        }
        byte[] data = Crypto.aesDecrypt(message.getEncryptedData().getData(), sharedKey);
        if (message.isCompressed()) {
            data = Convert.uncompress(data);
        }
        return data;
    }

    @Override
    public byte[] decryptUsingKeySeed(PrunableMessage message, byte[] keySeed) {
        if (message.getEncryptedData() == null) {
            return null;
        }
        byte[] publicKey = message.getSenderId() == AccountService.getId(Crypto.getPublicKey(keySeed))
            ? accountPublicKeyService.getPublicKeyByteArray(message.getRecipientId()) : accountPublicKeyService.getPublicKeyByteArray(message.getSenderId());
        return EncryptedDataUtil.decryptFrom(publicKey, message.getEncryptedData(), keySeed, message.isCompressed());
    }


    @Override
    public void add(Transaction transaction, PrunablePlainMessageAppendix appendix) {
        add(transaction, appendix, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

    @Override
    public void add(Transaction transaction, PrunablePlainMessageAppendix appendix, int blockTimestamp, int height) {
        if (appendix.getMessage() != null) {
            PrunableMessage prunableMessage = table.get(transaction.getId());
            if (prunableMessage == null) {
                prunableMessage = new PrunableMessage(transaction, blockTimestamp, height);
            } else if (prunableMessage.getHeight() != height) {
                throw new RuntimeException("Attempt to modify prunable message from height " + prunableMessage.getHeight() + " at height " + height);
            }
            if (prunableMessage.getMessage() == null) {
                prunableMessage.setPlain(appendix);
                table.insert(prunableMessage);
            }
        }
    }

    @Override
    public void add(Transaction transaction, PrunableEncryptedMessageAppendix appendix) {
        add(transaction, appendix, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

    @Override
    public void add(Transaction transaction, PrunableEncryptedMessageAppendix appendix, int blockTimestamp, int height) {
        if (appendix.getEncryptedData() != null) {
            PrunableMessage prunableMessage = table.get(transaction.getId());
            if (prunableMessage == null) {
                prunableMessage = new PrunableMessage(transaction, blockTimestamp, height);
            } else if (prunableMessage.getHeight() != height) {
                throw new RuntimeException("Attempt to modify prunable message from height " + prunableMessage.getHeight() + " at height " + height);
            }
            if (prunableMessage.getEncryptedData() == null) {
                prunableMessage.setEncrypted(appendix);
                table.insert(prunableMessage);
            }
        }
    }

    @Override
    public boolean isPruned(long transactionId, boolean hasPrunablePlainMessage, boolean hasPrunableEncryptedMessage) {
        return table.isPruned(transactionId, hasPrunablePlainMessage, hasPrunableEncryptedMessage);
    }
}
