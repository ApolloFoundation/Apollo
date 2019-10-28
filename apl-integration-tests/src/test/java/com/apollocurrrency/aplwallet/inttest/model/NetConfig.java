package com.apollocurrrency.aplwallet.inttest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NetConfig {
    private List<String> peers;
    private String chainId;
}
