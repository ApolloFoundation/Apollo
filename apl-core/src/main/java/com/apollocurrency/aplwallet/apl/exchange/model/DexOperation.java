package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
public class DexOperation {
    private Long dbId;
    private String account; // apl account in RS format
    private Stage stage;
    private String eid; // extra id column to search the database, eid + stage + account has to be unique to recover the state of dex
    private String description; // human readable description about operation
    private String details; // additional details required for troubleshooting
    private boolean finished; // indicates that operation was successfully finished
    private Timestamp ts; // timestamp of the operation (UNIX time since epoch without timezone)


    public enum Stage {
        NEW_ORDER(1), MATCHED_ORDER(2), ETH_DEPOSIT(3), ETH_SWAP(4), APL_CONTRACT_S1(5), APL_CONTRACT_S2(6), APL_CONTRACT_S3(7), APL_SWAP(8), ETH_REFUND(9), ETH_WITHDRAW(10);
        private final byte code;

        Stage(int code) {
            this.code = (byte) code;
        }

        public static Stage from(int code) {
            for (Stage stage : values()) {
                if (stage.code == code) {
                    return stage;
                }
            }
            throw new IllegalArgumentException("Operation was not found for code: " + code);
        }

        public byte getCode() {
            return code;
        }
    }
}
