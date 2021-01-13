package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.StateApiService;
import com.apollocurrency.aplwallet.api.v2.model.BlockInfo;
import com.apollocurrency.aplwallet.api.v2.model.BlockchainState;
import com.apollocurrency.aplwallet.apl.core.app.BlockNotFoundException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.LongParameter;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.BlockInfoMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FindTransactionService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Objects;

@RequestScoped
public class StateApiServiceImpl implements StateApiService {
    public static final Response NOT_IMPLEMENTED_RESPONSE = ResponseBuilderV2.apiError(ApiErrors.CUSTOM_ERROR_MESSAGE, "Not implemented yet, work in progress.").build();
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final TimeService timeService;
    private final BlockInfoMapper blockInfoMapper;
    private final FindTransactionService findTransactionService;
    private final TxReceiptMapper txReceiptMapper;

    @Inject
    public StateApiServiceImpl(BlockchainConfig blockchainConfig,
                               Blockchain blockchain,
                               TimeService timeService,
                               BlockInfoMapper blockInfoMapper,
                               FindTransactionService findTransactionService,
                               TxReceiptMapper txReceiptMapper) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.timeService = Objects.requireNonNull(timeService);
        this.blockInfoMapper = Objects.requireNonNull(blockInfoMapper);
        this.findTransactionService = Objects.requireNonNull(findTransactionService);
        this.txReceiptMapper = Objects.requireNonNull(txReceiptMapper);
    }

    public Response getBlockByHeight(String heightStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        int height;
        Block block;
        if (heightStr == null) {
            height = -1;
        } else {
            try {
                height = Integer.parseInt(heightStr);
            } catch (NumberFormatException e) {
                return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_PARAM, "height", heightStr).build();
            }
        }
        int blockChainHeight = blockchain.getHeight();
        if (height == -1 || 0 < height && height <= blockChainHeight) {
            try {
                if (height == -1) {
                    block = blockchain.getLastBlock();
                } else {
                    block = blockchain.getBlockAtHeight(height);
                }
            } catch (BlockNotFoundException e) {
                throw new NotFoundException(e.getMessage());
            }

            BlockInfo response = blockInfoMapper.convert(block);

            return builder.bind(response).build();
        } else {
            return ResponseBuilderV2.apiError(ApiErrors.OUT_OF_RANGE_NAME_VALUE,
                "height", heightStr, 0, blockChainHeight).build();
        }
    }

    public Response getBlockById(String blockIdStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        LongParameter blockId = new LongParameter(blockIdStr);
        Block block = blockchain.getBlock(blockId.get());
        if (block == null) {
            throw new NotFoundException("There is no block with id=" + blockIdStr);
        }

        BlockInfo response = blockInfoMapper.convert(block);

        return builder.bind(response).build();
    }

    public Response getBlockchainState(SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        if (!blockchain.isInitialized()) {
            return builder.error(ApiErrors.BLOCKCHAIN_NOT_INITIALIZED).build();
        }
        BlockchainState blockchainState = new BlockchainState();
        blockchainState.setChainid(blockchainConfig.getChain().getChainId().toString());
        blockchainState.setGenesisAccount(Long.toUnsignedString(GenesisImporter.CREATOR_ID));
        blockchainState.setGenesisBlockTimestamp(GenesisImporter.EPOCH_BEGINNING);
        blockchainState.setGenesisBlockId(Long.toUnsignedString(blockchain.getBlockIdAtHeight(0)));

        int height = blockchain.getHeight();
        blockchainState.setHeight((long) height);
        EcBlockData ecBlockData = blockchain.getECBlock(height);
        blockchainState.setEcBlockId(Long.toUnsignedString(ecBlockData.getId()));
        blockchainState.setEcBlockHeight((long) ecBlockData.getHeight());

        blockchainState.setTxTimestamp((long) timeService.getEpochTime());
        blockchainState.setTimestamp(timeService.systemTimeMillis());

        return builder.bind(blockchainState).build();
    }

    public Response getTxReceiptById(String transactionStr, SecurityContext securityContext) throws NotFoundException {

        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        final LongParameter transactionId = new LongParameter(transactionStr);

        Transaction transaction = findTransactionService.findTransaction(transactionId.get(), Integer.MAX_VALUE)
            .or(() -> findTransactionService.findUnconfirmedTransaction(transactionId.get()))
            .orElseThrow(() -> new NotFoundException("Thereis no transaction with id=" + transactionStr));

        return builder.bind(txReceiptMapper.convert(transaction)).build();
    }

    public Response getTxReceiptList(List<String> body, SecurityContext securityContext) throws NotFoundException {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    public Response getUnconfirmedTx(Integer page, Integer perPage, SecurityContext securityContext) throws NotFoundException {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    public Response getUnconfirmedTxCount(SecurityContext securityContext) throws NotFoundException {
        return NOT_IMPLEMENTED_RESPONSE;
    }
}
