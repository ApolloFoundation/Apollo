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

import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.PaymentTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;

public interface Attachment extends Appendix {

    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return PaymentTransactionType.ORDINARY;
        }

    };
    EmptyAttachment PRIVATE_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return PaymentTransactionType.PRIVATE;
        }

    };
    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return Messaging.ARBITRARY_MESSAGE;
        }

    };

    public TransactionType getTransactionType();

}
