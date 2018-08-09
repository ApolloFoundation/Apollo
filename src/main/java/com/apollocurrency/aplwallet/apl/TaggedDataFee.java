/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

/**
 * This class manages Fee for Tagged Data (uploaded files) and
 * it's historical modifications
 */
public class TaggedDataFee implements Fee {

    /**
     * Height at which TTL based fee is applied
     */
    private static final int TTL_FEE_ESTABLISHED_CHAIN_HEIGHT = 150000;

    /**
     * Old fee. Calculated for transactions before TTL_FEE_ESTABLISHED_CHAIN_HEIGHT
     */
    private static final Fee TAGGED_DATA_FEE_WITHOUT_TTL = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL/10) {
        @Override
        public int getSize(TransactionImpl transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    /**
     * new fee. Calculated for transactions at TTL_FEE_ESTABLISHED_CHAIN_HEIGHT
     */
    private static final Fee TAGGED_DATA_FEE_WITH_TTL = new DataStorageFee(Constants.ONE_APL, 2 * Constants.ONE_APL, 2 * Constants.ONE_APL);

    /**
     * Calculate fee for TaggedData.
     *
     * @param transaction
     * @param appendage
     * @return
     */
    @Override
    public long getFee(TransactionImpl transaction, Appendix appendage) {
        if(transaction.getHeight() < TTL_FEE_ESTABLISHED_CHAIN_HEIGHT){
            return TAGGED_DATA_FEE_WITHOUT_TTL.getFee(transaction, appendage);
        }
        return TAGGED_DATA_FEE_WITH_TTL.getFee(transaction, appendage);
    }

    /**
     *  DataStorageFee new fee, based on Size of a file and it's time-to-live
     */
    private static class DataStorageFee extends Fee.SizeAndTimeToLiveBasedFee {

        public DataStorageFee(long constantFee, long feePerSize, long feePerTimeUnit) {
            super(constantFee, feePerSize, feePerTimeUnit);
        }

        @Override
        public int getSize(TransactionImpl transaction, Appendix appendage) {
            return appendage.getFullSize();
        }

        @Override
        public long getTimeToLive(TransactionImpl transaction, Appendix appendage) {
            return transaction.getPrunableTimeToLive();
        }

    }

}