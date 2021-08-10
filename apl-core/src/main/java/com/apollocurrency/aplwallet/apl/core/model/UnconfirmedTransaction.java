/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;

import java.util.Map;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SET_PHASING_ONLY;

public class UnconfirmedTransaction extends WrappedTransaction {
    private final long arrivalTimestamp;
    private final long feePerByte;
    private final int fullSize;

/*    public UnconfirmedTransaction(Transaction transaction, long arrivalTimestamp) {
        this(transaction, arrivalTimestamp, transaction.getFeeATM() / transaction.getFullSize());
    }*/

    public UnconfirmedTransaction(Transaction transaction, long arrivalTimestamp, long feePerByte, int fullSize) {
        super(transaction);
        this.arrivalTimestamp = arrivalTimestamp;
        this.feePerByte = feePerByte;
        this.fullSize = fullSize;
    }

    public long getArrivalTimestamp() {
        return arrivalTimestamp;
    }

    public long getFeePerByte() {
        return feePerByte;
    }

    public int getFullSize() {
        return fullSize;
    }

    @Override
    public void setBlock(Block block) {
        throw new UnsupportedOperationException("Incorrect method 'setBlock()' call on 'unconfirmed' transaction instance.");
    }

    @Override
    public void unsetBlock() {
        throw new UnsupportedOperationException("Incorrect method 'unsetBlock()' call on 'unconfirmed' transaction instance.");
    }

    @Override
    public void setFeeATM(long feeATM) {
        if (transaction.getSignature() != null) {
            throw new UnsupportedOperationException("Unable to set fee for already signed transaction");
        } else {
            transaction.setFeeATM(feeATM);
        }
    }

    @Override
    public short getIndex() {
        return -1;
    }

    @Override
    public void setIndex(int index) {
        //index ignored for Unconfirmed transaction instance.
    }

    /**
     * @deprecated see method with longer parameters list below
     */
    @Override
    public boolean attachmentIsDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        if (!transaction.attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            // all are checked at acceptance height for block duplicates
            if (transaction.getType().isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return transaction.getType().isDuplicate(this, duplicates);
    }

    @Override
    public boolean attachmentIsDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates,
                                         boolean atAcceptanceHeight,
                                         Set<AccountControlType> senderAccountControls,
                                         AccountControlPhasing accountControlPhasing) {
        if (!transaction.attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            if (this.isBlockDuplicate(
                this, duplicates, senderAccountControls, accountControlPhasing)) {
                return true;
            }
            // all are checked at acceptance height for block duplicates
            if (transaction.getType().isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return transaction.getType().isDuplicate(this, duplicates);
    }

    private boolean isBlockDuplicate(Transaction transaction,
                                     Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates,
                                     Set<AccountControlType> senderAccountControls,
                                     AccountControlPhasing accountControlPhasing) {
        return
            senderAccountControls.contains(AccountControlType.PHASING_ONLY)
                && (accountControlPhasing != null && accountControlPhasing.getMaxFees() != 0)
                && transaction.getType().getSpec() != SET_PHASING_ONLY
                && TransactionType.isDuplicate(SET_PHASING_ONLY,
                Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
    }

    @Override
    public String toString() {
        return "UnconfirmedTransaction{" +
            "transaction=" + transaction +
            ", arrivalTimestamp=" + arrivalTimestamp +
            ", feePerByte=" + feePerByte +
            '}';
    }
}
