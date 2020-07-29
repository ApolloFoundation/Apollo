package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.OperationApiService;
import com.apollocurrency.aplwallet.api.v2.model.QueryCountResult;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.api.v2.model.QueryResult;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FindTransactionService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

@RequestScoped
public class OperationApiServiceImpl implements OperationApiService {
    private final FindTransactionService findTransactionService;
    private final TxReceiptMapper txReceiptMapper;

    @Inject
    public OperationApiServiceImpl(FindTransactionService findTransactionService,
                                   TxReceiptMapper txReceiptMapper) {
        this.findTransactionService = Objects.requireNonNull(findTransactionService);
        this.txReceiptMapper = Objects.requireNonNull(txReceiptMapper);
    }

    public Response getOperations(QueryObject body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        QueryResult result = new QueryResult();
        result.setQuery(body);
        QueryObject query = setDefaults(body);
        result.setResult(
            findTransactionService.getTransactionsByPeriod(query.getStartTime().intValue(), query.getEndTime().intValue())
        );
        return builder.bind(result).build();
    }

    public Response getOperationsCount(QueryObject body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        QueryCountResult result = new QueryCountResult();
        result.setQuery(body);
        QueryObject query = setDefaults(body);
        result.setCount(
            findTransactionService.getTransactionsCountByPeriod(query.getStartTime().intValue(), query.getEndTime().intValue())
        );
        return builder.bind(result).build();
    }

    private QueryObject setDefaults(QueryObject query) {
        QueryObject result = new QueryObject();

        query.getAccounts();

        result.setStartTime(query.getStartTime() != null ? query.getStartTime() : -1L);
        result.setEndTime(query.getEndTime() != null ? query.getEndTime() : -1L); //timestamp
        result.setFirst(query.getFirst() != null ? query.getFirst() : -1L);
        result.setLast(query.getLast() != null ? query.getLast() : -1L); //timestamp

        query.getPage();
        query.getPerPage();
        query.getOrderBy();

        return result;
    }
}
