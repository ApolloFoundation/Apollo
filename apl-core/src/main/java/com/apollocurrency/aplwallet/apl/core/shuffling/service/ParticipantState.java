/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import java.util.Arrays;

public enum ParticipantState {
    REGISTERED((byte)0, new byte[]{1}),
    PROCESSED((byte)1, new byte[]{2,3}),
    VERIFIED((byte)2, new byte[]{3}),
    CANCELLED((byte)3, new byte[]{});

    private final byte code;
    private final byte[] allowedNext;

    ParticipantState(byte code, byte[] allowedNext) {
        this.code = code;
        this.allowedNext = allowedNext;
    }

    public static ParticipantState get(byte code) {
        for (ParticipantState state : ParticipantState.values()) {
            if (state.code == code) {
                return state;
            }
        }
        throw new IllegalArgumentException("No matching state for " + code);
    }

    public byte getCode() {
        return code;
    }

    public boolean canBecome(ParticipantState nextState) {
        return Arrays.binarySearch(allowedNext, nextState.code) >= 0;
    }
}
