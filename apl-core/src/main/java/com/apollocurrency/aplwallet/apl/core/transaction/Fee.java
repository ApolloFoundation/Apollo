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
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.smc.contract.Payable;
import com.apollocurrency.smc.contract.fuel.FuelCalculator;

import java.math.BigInteger;
import java.util.Objects;

public interface Fee {

    Fee NONE = new Fee.ConstantFee();

    long getFee(Transaction transaction, Appendix appendage);

    final class ConstantFee implements Fee {

        private final long fee;

        public ConstantFee(long fee) {
            this.fee = fee;
        }

        public ConstantFee() {
            this.fee = 0;
        }

        @Override
        public long getFee(Transaction transaction, Appendix appendage) {
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
        public final long getFee(Transaction transaction, Appendix appendage) {
            int size = getSize(transaction, appendage) - 1;
            if (size < 0) {
                return constantFee;
            }
            return Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize));
        }

        public abstract int getSize(Transaction transaction, Appendix appendage);

    }

    abstract class FuelBasedConstantFee implements Fee {

        private final FuelCalculator fuelCalculator;

        public FuelBasedConstantFee(FuelCalculator fuelCalculator) {
            this.fuelCalculator = Objects.requireNonNull(fuelCalculator);
        }

        @Override
        public final long getFee(Transaction transaction, Appendix appendage) {
            return getFuelPrice(transaction, appendage).multiply(fuelCalculator.calc()).longValueExact();
        }

        public abstract BigInteger getFuelPrice(Transaction transaction, Appendix appendage);
    }

    abstract class FuelBasedFee implements Fee {

        private final FuelCalculator fuelCalculator;

        public FuelBasedFee(FuelCalculator fuelCalculator) {
            this.fuelCalculator = Objects.requireNonNull(fuelCalculator);
        }

        @Override
        public final long getFee(Transaction transaction, Appendix appendage) {
            final int size = getSize(transaction, appendage);
            return getFuelPrice(transaction, appendage).multiply(fuelCalculator.calc(() -> size)).longValueExact();
        }

        /**
         * Returns the value equal to fuel needs to successfully publish or call the contract.
         *
         * @param value the given payable object
         * @return the value equal to fuel needs to successfully publish or call the contract.
         */
        public BigInteger calcFuel(Payable value) {
            final int size = value.getPayableSize();
            return fuelCalculator.calc(() -> size);
        }

        public abstract int getSize(Transaction transaction, Appendix appendage);

        public abstract BigInteger getFuelPrice(Transaction transaction, Appendix appendage);
    }

}
