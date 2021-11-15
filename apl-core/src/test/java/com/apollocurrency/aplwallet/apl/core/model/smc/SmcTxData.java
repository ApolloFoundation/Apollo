/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
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

    public AplAddress getSenderAddress() {
        return new AplAddress(Convert.parseAccountId(sender));
    }

    public AplAddress getRecipientAddress() {
        return new AplAddress(Convert.parseAccountId(recipient));
    }

    public AplAddress getContractAddress() {
        return new AplAddress(Convert.parseAccountId(address));
    }
}
