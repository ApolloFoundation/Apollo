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
    private String account;
    private Stage stage;
    private String description;
    private String details;
    private Timestamp time;


    public enum Stage {
        NEW_ORDER(1), MATCHED_ORDER(2), ETH_DEPOSIT(4), ETH_SWAP(8), APL_CONTRACT_S1(16), APL_CONTRACT_S2(32), APL_CONTRACT_S3(64), APL_SWAP(128), ETH_REFUND(256), ETH_WITHDRAW(512);
        private final int code;

        Stage(int code) {
            this.code = code;
        }

        public static Stage from(int code) {
            for (Stage stage : values()) {
                if (stage.code == code) {
                    return stage;
                }
            }
            throw new IllegalArgumentException("Operation was not found for code: " + code);
        }
    }
}
