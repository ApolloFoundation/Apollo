package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.TransactionApiService;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.ListResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.TxRequest;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TransactionInfoMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequestScoped
public class TransactionApiServiceImpl implements TransactionApiService {

    private final Blockchain blockchain;
    private final TxReceiptMapper txReceiptMapper;
    private final TransactionInfoMapper transactionInfoMapper;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final MemPool memPool;
    private final TxBContext txBContext;
    private final TransactionProcessor processor;

    @Inject
    public TransactionApiServiceImpl(MemPool memPool,
                                     BlockchainConfig blockchainConfig,
                                     Blockchain blockchain,
                                     TxReceiptMapper txReceiptMapper,
                                     TransactionInfoMapper transactionInfoMapper, TransactionBuilderFactory transactionBuilderFactory,
                                     TransactionProcessor processor) {
        this.memPool = Objects.requireNonNull(memPool);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.txReceiptMapper = Objects.requireNonNull(txReceiptMapper);
        this.transactionInfoMapper = Objects.requireNonNull(transactionInfoMapper);
        this.transactionBuilderFactory = transactionBuilderFactory;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
        this.processor = processor;
    }

    /*
     * response=UnTxReceipt
     */
    public Response broadcastTx(TxRequest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        if (memPool.pendingProcessingRemainingCapacity() <= 0) {
            return ResponseBuilderV2.apiError(ApiErrors.UNCONFIRMED_TRANSACTION_CACHE_IS_FULL).status(409).build();
        }
        StatusResponse rc = broadcastOneTx(body);
        return builder.bind(rc.getResponse()).status(rc.getStatus()).build();
    }

    public Response broadcastTxBatch(List<TxRequest> body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        if (memPool.pendingProcessingRemainingCapacity() < body.size()) {
            return ResponseBuilderV2.apiError(ApiErrors.UNCONFIRMED_TRANSACTION_CACHE_IS_FULL).status(409).build();
        }
        ListResponse listResponse = new ListResponse();
        for (TxRequest req : body) {
            BaseResponse receipt = broadcastOneTx(req).getResponse();
            listResponse.getResult().add(receipt);
        }
        return builder.bind(listResponse).build();
    }

    private StatusResponse broadcastOneTx(TxRequest req) {
        int status = 200;
        BaseResponse receipt;
        try {
            if (log.isTraceEnabled()) {
                log.trace("API_V2: Broadcast transaction: tx={}", req.getTx());
            }
            byte[] tx = Convert.parseHexString(req.getTx());
            Transaction newTx = transactionBuilderFactory.newTransaction(tx);

            Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(newTx.getVersion()).serialize(newTx, signedTxBytes);

            if (log.isTraceEnabled()) {
                log.trace("API_V2: parsed transaction=[{}] attachment={}", newTx.getType(), newTx.getAttachment());
                log.trace(" Given {}", req.getTx());
                log.trace("Actual {}", Convert.toHexString(signedTxBytes.array()));
            }

            processor.broadcast(newTx);
            receipt = txReceiptMapper.convert(newTx);
            if (log.isTraceEnabled()) {
                log.trace("API_V2: UnTxReceipt={}", receipt);
            }
        } catch (NumberFormatException e) {
            receipt = ResponseBuilderV2.createErrorResponse(
                ApiErrors.CUSTOM_ERROR_MESSAGE, null,
                "Can't parse byte array, cause " + e.getMessage());
            status = 400;

        } catch (AplException.ValidationException e) {
            receipt = ResponseBuilderV2.createErrorResponse(
                ApiErrors.CONSTRAINT_VIOLATION, null,
                e.getMessage());
            status = 400;
        }
        return new StatusResponse(status, receipt);
    }

    public Response getTxById(String transactionIdStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        long transactionId;
        Transaction transaction;
        try {
            transactionId = Convert.parseUnsignedLong(transactionIdStr);
        } catch (RuntimeException e) {
            return builder
                .error(ApiErrors.CUSTOM_ERROR_MESSAGE, "Can't parse transaction id, cause " + e.getMessage())
                .build();
        }
        transaction = blockchain.getTransaction(transactionId);
        if (transaction == null) {
            transaction = memPool.get(transactionId);
        }
        if (transaction == null) {
            throw new NotFoundException("Transaction not found. id=" + transactionIdStr);
        }
        TransactionInfoResp resp = transactionInfoMapper.convert(transaction);
        if (log.isTraceEnabled()) {
            log.trace("TransactionResp={}", resp);
        }
        return builder.bind(resp).build();
    }

    @Getter
    private static class StatusResponse {
        int status;
        BaseResponse response;

        public StatusResponse(int status, BaseResponse response) {
            this.status = status;
            this.response = response;
        }
    }
}
