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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

public interface Fee {

    long getFee(TransactionImpl transaction, Appendix appendage);

    Fee DEFAULT_FEE = new Fee.ConstantFee(Constants.ONE_APL);

    Fee NONE = new Fee.ConstantFee();

    final class ConstantFee implements Fee {

        private final long fee;

        public ConstantFee(long fee) {
            this.fee = fee;
        }

        public ConstantFee() {
            this.fee = 0;
        }

        @Override
        public long getFee(TransactionImpl transaction, Appendix appendage) {
            return fee;
        }

    }

    abstract class SizeBasedFee implements Fee {

        private final long constantFee;
        private final long feePerSize;
        private final int unitSize;

        public SizeBasedFee(long feePerSize) {
            this(0, feePerSize);
        }

        public SizeBasedFee(long constantFee, long feePerSize) {
            this(constantFee, feePerSize, 1024);
        }

        public SizeBasedFee(long constantFee, long feePerSize, int unitSize) {
            this.constantFee = constantFee;
            this.feePerSize = feePerSize;
            this.unitSize = unitSize;
        }

        // the first size unit is free if constantFee is 0
        @Override
        public final long getFee(TransactionImpl transaction, Appendix appendage) {
            int size = getSize(transaction, appendage) - 1;
            if (size < 0) {
                return constantFee;
            }
            return Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize));
        }

        public abstract int getSize(TransactionImpl transaction, Appendix appendage);

    }

    /**
     * Fee based on size of an object and time-to-live of this object
     */
    abstract class SizeAndTimeToLiveBasedFee implements Fee {

        private final long constantFee;
        private final long feePerSize;
        private final int unitSize;

        private final long feePerTimeUnit;
        private final long timeUnitSize; // in seconds

        public SizeAndTimeToLiveBasedFee(long constantFee, long feePerSize, long feePerTimeUnit) {
            this(constantFee, feePerSize, 1024, feePerTimeUnit, 60 * 60 * 24 * 30 /* one month */);
        }

        public SizeAndTimeToLiveBasedFee(long constantFee, long feePerSize, int unitSize, long feePerTimeUnit, long timeUnitSize) {
            this.constantFee = constantFee;
            this.feePerSize = feePerSize;
            this.unitSize = unitSize;
            this.feePerTimeUnit = feePerTimeUnit;
            this.timeUnitSize = timeUnitSize;
        }

        // the first size unit is free if constantFee is 0
        @Override
        public final long getFee(TransactionImpl transaction, Appendix appendage) {
            int size = getSize(transaction, appendage) - 1;
            if (size < 0) {
                size = 0;
            }
            long ttl = getTimeToLive(transaction, appendage);
            if(ttl < 0) {
                ttl = 0;
            }

            long sizeFee = Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize));
            long ttlFee = Math.multiplyExact(ttl / timeUnitSize, feePerTimeUnit);
            return Math.addExact(sizeFee, ttlFee);
        }

        public abstract int getSize(TransactionImpl transaction, Appendix appendage);
        public abstract long getTimeToLive(TransactionImpl transaction, Appendix appendage);

    }

}
