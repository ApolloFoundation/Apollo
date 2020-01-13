package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DexTransaction {
    private Long dbId;
    private byte[] hash;
    private byte[] rawTransactionBytes;
    private Op operation;
    private String params;
    private String account;
    private long timestamp;

    public enum Op {
        DEPOSIT((byte) 0),INITIATE((byte)1), REDEEM((byte)2), REFUND((byte)3), WITHDRAW((byte)4);
        final byte code;

        public byte getCode() {
            return code;
        }

        Op(byte code) {
            this.code = code;
        }

        public static Op from(byte code) {
            for (Op value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("DexOperation for code '" + code + "' does not exist");
        }
    }
}
