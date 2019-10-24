package com.apollocurrrency.aplwallet.inttest.model;

import lombok.*;

import java.util.ArrayList;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NetConfig {
    private ArrayList<String> peers;
    private String chainId;
}
