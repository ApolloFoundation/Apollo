/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;

import java.util.Comparator;

public class UnconfirmedTransactionComparator implements Comparator<UnconfirmedTransaction> {
    @Override
    public int compare(UnconfirmedTransaction o1, UnconfirmedTransaction o2) {
        int result;
        if ((result = Integer.compare(o2.getHeight(), o1.getHeight())) != 0) {
            return result;
        }
        if ((result = Boolean.compare(o2.getTransactionImpl().referencedTransactionFullHash() != null,
                o1.getTransactionImpl().referencedTransactionFullHash() != null)) != 0) {
            return result;
        }
        if ((result = Long.compare(o1.getFeePerByte(), o2.getFeePerByte())) != 0) {
            return result;
        }
        if ((result = Long.compare(o2.getArrivalTimestamp(), o1.getArrivalTimestamp())) != 0) {
            return result;
        }
        return Long.compare(o2.getId(), o1.getId());
    }
}
