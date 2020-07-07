package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.TransactionApiService;
import com.apollocurrency.aplwallet.api.v2.model.ListResponse;
import com.apollocurrency.aplwallet.api.v2.model.TxRequest;
import com.apollocurrency.aplwallet.api.v2.model.UnTxReceipt;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.UnTxReceiptMapper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Objects;

@RequestScoped
@Slf4j
public class TransactionApiServiceImpl implements TransactionApiService {

    private final UnTxReceiptMapper unTxReceiptMapper;
    private final TransactionProcessor transactionProcessor;

    @Inject
    public TransactionApiServiceImpl(TransactionProcessor transactionProcessor, UnTxReceiptMapper unTxReceiptMapper) {
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.unTxReceiptMapper = Objects.requireNonNull(unTxReceiptMapper);
    }

    /*
     * response=UnTxReceipt
     */
    public Response broadcastTx(TxRequest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        String txString = body.getTx();
        try {
            if(log.isTraceEnabled()){
                log.trace("Broadcast transaction: body={}", body);
            }
            byte[] tx = Convert.parseHexString(txString);
            Transaction.Builder txBuilder = Transaction.newTransactionBuilder(tx);
            Transaction newTx = txBuilder.build();
            transactionProcessor.broadcast(newTx);
            UnTxReceipt receipt = unTxReceiptMapper.convert(newTx);
            if(log.isTraceEnabled()) {
                log.trace("UnTxReceipt={}", receipt);
            }
            return builder.bind(receipt).build();
        } catch (NumberFormatException e){
            return builder
                .error(ApiErrors.CUSTOM_ERROR_MESSAGE, "Cant't parse byte array, cause "+e.getMessage())
                .build();
        } catch (AplException.ValidationException e) {
            return builder
                .error(ApiErrors.CONSTRAINT_VIOLATION, e.getMessage())
                .build();
        }
    }

    public Response broadcastTxBatch(List<TxRequest> body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        ListResponse listResponse = new ListResponse();
        for(TxRequest req: body) {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Byte array tx={}", req.getTx());
                }
                byte[] tx = Convert.parseHexString(req.getTx());
                Transaction.Builder txBuilder = Transaction.newTransactionBuilder(tx);
                Transaction newTx = txBuilder.build();
                transactionProcessor.broadcast(newTx);
                UnTxReceipt receipt = unTxReceiptMapper.convert(newTx);
                if (log.isTraceEnabled()) {
                    log.trace("UnTxReceipt={}", receipt);
                }
                listResponse.getResult().add(receipt);
            } catch (NumberFormatException e) {
                listResponse.getResult().add(
                    ResponseBuilderV2.createErrorResponse(
                        ApiErrors.CUSTOM_ERROR_MESSAGE,
                        "Cant't parse byte array, cause " + e.getMessage()));

            } catch (AplException.ValidationException e) {
                listResponse.getResult().add(
                    ResponseBuilderV2.createErrorResponse(
                    ApiErrors.CONSTRAINT_VIOLATION,
                        e.getMessage()));
            }
        }
        return builder.bind(listResponse).build();
    }

    public Response getTxById(String transaction, SecurityContext securityContext) throws NotFoundException {
        return Response.ok().build();
    }
}
