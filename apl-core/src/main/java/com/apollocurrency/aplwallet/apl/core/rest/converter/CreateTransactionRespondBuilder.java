/*
 * Copyright (c)  2018-2010 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CreateTransactionRespondBuilder {

    private CreateTransactionRequest txRequest;
    private Transaction transaction;
    private UnconfirmedTransactionDTO txDto;

    public CreateTransactionResponse build() {
        CreateTransactionResponse createTransactionResponse = new CreateTransactionResponse();
        createTransactionResponse.setTransactionJSON(txDto);
        createTransactionResponse.setUnsignedTransactionBytes(Convert.toHexString(transaction.getUnsignedBytes()));
        createTransactionResponse.setTransaction(transaction.getStringId());
        createTransactionResponse.setFullHash(transaction.getFullHashString());
        createTransactionResponse.setTransactionBytes(Convert.toHexString(transaction.getBytes()));
        createTransactionResponse.setBroadcasted(txRequest.isBroadcast());
        return createTransactionResponse;
    }
}
