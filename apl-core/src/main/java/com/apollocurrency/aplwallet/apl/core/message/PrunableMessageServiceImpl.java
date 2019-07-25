/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.message;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.util.List;
import javax.inject.Singleton;

@Singleton
public class PrunableMessageServiceImpl implements PrunableMessageService {
    private PrunableMessageTable table;
    private Blockchain blockchain;


    public int getCount() {
        return table.getCount();
    }

    public List<PrunableMessage> getAll(int from, int to) {
        return CollectionUtil.toList(table.getAll(from, to));
    }

    public PrunableMessage getPrunableMessage(long transactionId) {
        return table.get(transactionId);
    }

    public List<PrunableMessage> getPrunableMessages(long accountId, int from, int to) {
        return table.getPrunableMessages(accountId, from, to);
    }

    public List<PrunableMessage> getPrunableMessages(long accountId, long otherAccountId, int from, int to) {
        return table.getPrunableMessages(accountId, otherAccountId, from, to);
    }

    public byte[] decrypt(PrunableMessage message, String secretPhrase) {
        return decryptUsingKeySeed(message, Crypto.getKeySeed(secretPhrase));
    }

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

    public byte[] decryptUsingKeySeed(PrunableMessage message, byte[] keySeed) {
        if (message.getEncryptedData() == null) {
            return null;
        }
        byte[] publicKey = message.getSenderId() == Account.getId(Crypto.getPublicKey(keySeed))
                ? Account.getPublicKey(message.getRecipientId()) : Account.getPublicKey(message.getSenderId());
        return Account.decryptFrom(publicKey, message.getEncryptedData(), keySeed, message.isCompressed());
    }


    public void add(Transaction transaction, PrunablePlainMessageAppendix appendix) {
        add(transaction, appendix, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

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

    public void add(Transaction transaction, PrunableEncryptedMessageAppendix appendix) {
        add(transaction, appendix, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

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

    public boolean isPruned(long transactionId, boolean hasPrunablePlainMessage, boolean hasPrunableEncryptedMessage) {
        return table.isPruned(transactionId, hasPrunablePlainMessage, hasPrunableEncryptedMessage);
    }
}
