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

package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public interface Appendix {

    public int getSize();
    public int getFullSize();
    public void putBytes(ByteBuffer buffer);
    public JSONObject getJSONObject();
    public byte getVersion();
    public int getBaselineFeeHeight();
    public Fee getBaselineFee(Transaction transaction);
    public int getNextFeeHeight();
    public Fee getNextFee(Transaction transaction);
    public default boolean isPhasable() {return false;}
    public boolean isPhased(Transaction transaction);
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount);
    public void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException;
    public void validate(Transaction transaction, int blockHeight ) throws AplException.ValidationException;

    public static boolean hasAppendix(String appendixName, JSONObject attachmentData) {
        return attachmentData.get("version." + appendixName) != null;
    }

    public default void loadPrunable(Transaction transaction) {}
    public default void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {}
    public default String getAppendixName() { return null;}

}
