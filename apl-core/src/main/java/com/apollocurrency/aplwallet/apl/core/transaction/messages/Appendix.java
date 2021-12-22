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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.WriteBuffer;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public interface Appendix {

    static boolean hasAppendix(String appendixName, JSONObject attachmentData) {
        return attachmentData.get("version." + appendixName) != null;
    }

    /**
     * Returns the appendix size in bytes.
     * <i>fullSize</i> can be less or equal than <i>size</i>
     *
     * @return size in bytes
     */
    int getSize();

    /**
     * Returns the size of payload i.e. payable transaction part.
     * <i>fullSize</i> can be less or equal than <i>size</i>
     *
     * @return size in bytes
     */
    int getFullSize();

    void putBytes(WriteBuffer buffer);

    void putBytes(ByteBuffer buffer);

    JSONObject getJSONObject();

    /**
     * Returns appendix flag as a bit mask.
     * 0x00 - AbstractAttachment
     * 0x01 - Message
     * 0x02 - EncryptedMessage
     * 0x04 - PublicKeyAnnouncement
     * 0x08 - EncryptToSelfMessage
     * 0x10 - Phasing
     * 0x20 - PrunablePlainMessage
     * 0x40 - PrunableEncryptedMessage
     *
     * @return appendix flag
     */
    default int getAppendixFlag(){
        return 0;
    }

    byte getVersion();

    Fee getBaselineFee(Transaction transaction, long oneAPL);

    default boolean isPhasable() {
        return false;
    }

    boolean isPhased(Transaction transaction);

    void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException;

    void performStateDependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException;

    void performStateIndependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException;

    default String getAppendixName() {
        return null;
    }

    void undo(Transaction transaction, Account senderAccount, Account recipientAccount);
}
