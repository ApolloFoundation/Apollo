/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.TxErrorHashDTO;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import jakarta.enterprise.inject.Vetoed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convert block model into a block dto using configured properties to either add txs or phased txs or not.
 * <p><b>ATTENTION!</b> This class should NOT be SINGLETON, since such case may cause configuration issues between classes, sharing same instance.
 * <br>
 * Each caller should instantiate new instance or just use CDI default Dependant Scope</p>
 */
@Vetoed
public class BlockConverter implements Converter<Block, BlockDTO> {

    private final Blockchain blockchain;
    private final TransactionConverter transactionConverter;
    private final PhasingPollService phasingPollService;
    private volatile boolean isAddTransactions = false;
    private volatile boolean isAddPhasedTransactions = false;

    public BlockConverter(Blockchain blockchain, TransactionConverter transactionConverter,
                          PhasingPollService phasingPollService) {
        this.blockchain = blockchain;
        this.transactionConverter = transactionConverter;
        this.phasingPollService = phasingPollService;
    }

    @Override
    public BlockDTO apply(Block model) {

        BlockDTO dto = new BlockDTO();
        blockchain.loadBlockData(model);
        dto.setBlock(model.getStringId());
        dto.setHeight(model.getHeight());
        dto.setGenerator(Long.toUnsignedString(model.getGeneratorId()));
        dto.setGeneratorRS(Convert2.rsAccount(model.getGeneratorId()));
        dto.setGeneratorPublicKey(Convert.toHexString(model.getGeneratorPublicKey()));
        dto.setTimestamp(model.getTimestamp());
        dto.setTimeout(model.getTimeout());
        dto.setTotalFeeATM(String.valueOf(model.getTotalFeeATM()));
        dto.setPayloadLength(model.getPayloadLength());
        dto.setVersion(model.getVersion());
        dto.setBaseTarget(String.valueOf(model.getBaseTarget()));
        dto.setCumulativeDifficulty(model.getCumulativeDifficulty().toString());
        if (model.getPreviousBlockId() != 0) {
            dto.setPreviousBlock(Long.toUnsignedString(model.getPreviousBlockId()));
        }
        dto.setPreviousBlockHash(Convert.toHexString(model.getPreviousBlockHash()));
        if (model.getNextBlockId() != 0) {
            dto.setNextBlock(Long.toUnsignedString(model.getNextBlockId()));
        }
        dto.setPayloadHash(Convert.toHexString(model.getPayloadHash()));
        dto.setGenerationSignature(Convert.toHexString(model.getGenerationSignature()));
        dto.setBlockSignature(Convert.toHexString(model.getBlockSignature()));
        dto.setTransactions(Collections.emptyList());
        this.addTransactions(dto, model);
        if (this.isAddPhasedTransactions) {
            this.addPhasedTransactions(dto, model);
        }
        dto.setNumberOfFailedTxs(model.getTxErrorHashes().size());
        model.getTxErrorHashes().forEach(e-> dto.getTxErrorHashes().add(new TxErrorHashDTO(Long.toUnsignedString(e.getId()), Convert.toHexString(e.getErrorHash()), e.getError())));
        return dto;
    }

    public void addTransactions(BlockDTO o, Block model) {
        if (o != null && model != null) {
            List<TransactionDTO> transactionDTOList = model.getTransactions().stream().map(transactionConverter).collect(Collectors.toList());
            o.setNumberOfTransactions((long) model.getTransactions().size());
            // we cant use here original block.totalAmountATM because of private transactions
            o.setTotalAmountATM(String.valueOf(transactionDTOList.stream().filter(e-> e.getErrorMessage() == null).map(TransactionDTO::getAmountATM).mapToLong(Long::parseLong).sum()));
            if (this.isAddTransactions) {
                o.setTransactions(transactionDTOList);
            }
        }
    }

    public void addPhasedTransactions(BlockDTO o, Block model) {
        if (o != null && model != null) {
            List<TransactionDTO> transactionDTOList = new ArrayList<>();
            List<Long> transactionIdList = phasingPollService.getApprovedTransactionIds(model.getHeight());
            transactionIdList
                .forEach(trId -> transactionDTOList
                    .add(transactionConverter.convert(blockchain.getTransaction(trId))));

            o.setExecutedPhasedTransactions(transactionDTOList);
        }
    }

    public void addPhasedTransactionIds(BlockDTO o, Block model) {
        if (o != null && model != null) {
            List<Long> approvedTransactionIds = phasingPollService.getApprovedTransactionIds(model.getHeight());
            List<String> transactionList = approvedTransactionIds.stream().map(Long::toUnsignedString).collect(Collectors.toList());
            o.setExecutedPhasedTransactions(transactionList);
        }
    }

    public boolean isAddTransactions() {
        return isAddTransactions;
    }

    public void setAddTransactions(boolean addTransactions) {
        isAddTransactions = addTransactions;
    }

    public boolean isAddPhasedTransactions() {
        return isAddPhasedTransactions;
    }

    public void setAddPhasedTransactions(boolean addPhasedTransactions) {
        isAddPhasedTransactions = addPhasedTransactions;
    }

    public void setPriv(boolean priv) {
        transactionConverter.setPriv(priv);
    }

    public void reset() {
        isAddTransactions = false;
        isAddPhasedTransactions = false;
        transactionConverter.setPriv(true);
    }


}
