package com.apollocurrrency.aplwallet.inttest.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NetConfig {
    private List<String> peers;
    private String chainId;
}
