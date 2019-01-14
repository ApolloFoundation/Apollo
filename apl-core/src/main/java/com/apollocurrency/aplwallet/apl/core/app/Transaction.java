/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.app.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.app.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.app.messages.EncryptToSelfMessage;
import com.apollocurrency.aplwallet.apl.core.app.messages.EncryptedMessage;
import com.apollocurrency.aplwallet.apl.core.app.messages.Message;
import com.apollocurrency.aplwallet.apl.core.app.messages.Phasing;
import com.apollocurrency.aplwallet.apl.core.app.messages.PrunableEncryptedMessage;
import com.apollocurrency.aplwallet.apl.core.app.messages.PrunablePlainMessage;
import com.apollocurrency.aplwallet.apl.core.app.messages.PublicKeyAnnouncement;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public interface Transaction {

    interface Builder {

        Builder recipientId(long recipientId);

        default Builder recipientRs(String recipientRS) {return recipientId(Convert.parseAccountId(recipientRS));}

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder appendix(Message message);

        Builder appendix(EncryptedMessage encryptedMessage);

        Builder appendix(EncryptToSelfMessage encryptToSelfMessage);

        Builder appendix(PublicKeyAnnouncement publicKeyAnnouncement);

        Builder appendix(PrunablePlainMessage prunablePlainMessage);

        Builder appendix(PrunableEncryptedMessage prunableEncryptedMessage);

        Builder appendix(Phasing phasing);

        Builder timestamp(int timestamp);

        Builder ecBlockHeight(int height);

        Builder ecBlockId(long blockId);

        Transaction build() throws AplException.NotValidException;

        Transaction build(byte[] keySeed) throws AplException.NotValidException;

    }

    long getId();

    String getStringId();

    long getSenderId();

    byte[] getSenderPublicKey();

    long getRecipientId();

    int getHeight();

    void setHeight(int height);

    long getBlockId();

    Block getBlock();

    void setBlock(Block block);

    void unsetBlock();

    short getIndex();

    void setIndex(int index);

    int getTimestamp();

    int getBlockTimestamp();

    short getDeadline();

    int getExpiration();

    long getAmountATM();

    long getFeeATM();

    String getReferencedTransactionFullHash();

    byte[] referencedTransactionFullHash();

    byte[] getSignature();

    String getFullHash();

    byte[] fullHash();

    TransactionType getType();

    Attachment getAttachment();

    boolean verifySignature();

    void validate() throws AplException.ValidationException;

    byte[] getBytes();

    byte[] bytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    JSONObject getPrunableAttachmentJSON();

    byte getVersion();

    int getFullSize();

    Message getMessage();

    EncryptedMessage getEncryptedMessage();

    EncryptToSelfMessage getEncryptToSelfMessage();

    Phasing getPhasing();

    boolean attachmentIsPhased();

    PublicKeyAnnouncement getPublicKeyAnnouncement();

    PrunablePlainMessage getPrunablePlainMessage();

    boolean hasPrunablePlainMessage();

    PrunableEncryptedMessage getPrunableEncryptedMessage();

    boolean hasPrunableEncryptedMessage();

    List<AbstractAppendix> getAppendages();

    List<AbstractAppendix> getAppendages(boolean includeExpiredPrunable);

    List<AbstractAppendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable);

    int getECBlockHeight();

    long getECBlockId();

    boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates, boolean atAcceptanceHeight);
}
