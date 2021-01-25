package com.apollocurrency.aplwallet.apl.core.rest.converter;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.api.dto.ExchangeContractDTO;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

@Singleton
public class ExchangeContractToDTOConverter implements Converter<ExchangeContract, ExchangeContractDTO> {
    @Override
    public ExchangeContractDTO apply(ExchangeContract exchangeContract) {
        ExchangeContractDTO exchangeContractDTO = new ExchangeContractDTO();
        exchangeContractDTO.setId(Long.toUnsignedString(exchangeContract.getId()));
        exchangeContractDTO.setOrderId(Long.toUnsignedString(exchangeContract.getOrderId()));
        exchangeContractDTO.setCounterOrderId(Long.toUnsignedString(exchangeContract.getCounterOrderId()));
        exchangeContractDTO.setContractStatus((byte) exchangeContract.getContractStatus().ordinal());
        exchangeContractDTO.setCounterTransferTxId(exchangeContract.getCounterTransferTxId());
        exchangeContractDTO.setTransferTxId(exchangeContract.getTransferTxId());
        exchangeContractDTO.setDeadlineToReply(exchangeContract.getDeadlineToReply());
        exchangeContractDTO.setEncryptedSecret(Convert.toHexString(exchangeContract.getEncryptedSecret()));
        exchangeContractDTO.setSecretHash(Convert.toHexString(exchangeContract.getSecretHash()));
        exchangeContractDTO.setSender(Convert2.defaultRsAccount(exchangeContract.getSender()));
        exchangeContractDTO.setRecipient(Convert2.defaultRsAccount(exchangeContract.getRecipient()));
        exchangeContractDTO.setHeight(exchangeContract.getHeight());
        return exchangeContractDTO;
    }
}
