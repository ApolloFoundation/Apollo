/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Builder
@Data
public class SmcTxData {
    String address;
    String sender;
    String recipient;
    String recipientPublicKey;
    String name;
    String method;
    String source;
    String secret;
    long amountATM;
    long fuelLimit;
    long fuelPrice;
    List<String> params;
}
