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

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.common.WriteBuffer;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public interface Appendix {

    static boolean hasAppendix(String appendixName, JSONObject attachmentData) {
        return attachmentData.get("version." + appendixName) != null;
    }

    int getSize();

    int getFullSize();

    void putBytes(WriteBuffer buffer);

    void putBytes(ByteBuffer buffer);

    JSONObject getJSONObject();

    byte getVersion();

    Fee getBaselineFee(Transaction transaction, long oneAPL);

    default boolean isPhasable() {
        return false;
    }

    boolean isPhased(Transaction transaction);

    void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException;

    void performFullValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException;

    void performLightweightValidation(Transaction transaction, int blockcHeight) throws AplException.ValidationException;

    default String getAppendixName() {
        return null;
    }

}
