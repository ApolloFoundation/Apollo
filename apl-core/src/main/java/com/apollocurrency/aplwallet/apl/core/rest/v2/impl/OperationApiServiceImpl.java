package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.OperationApiService;
import com.apollocurrency.aplwallet.api.v2.model.QueryCountResult;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.api.v2.model.QueryResult;
import com.apollocurrency.aplwallet.apl.core.model.AplQueryObject;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FindTransactionService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

@RequestScoped
public class OperationApiServiceImpl implements OperationApiService {
    private final FindTransactionService findTransactionService;

    @Inject
    public OperationApiServiceImpl(FindTransactionService findTransactionService) {
        this.findTransactionService = Objects.requireNonNull(findTransactionService);
    }

    public Response getOperations(QueryObject body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        QueryResult result = new QueryResult();
        result.setQuery(body);
        AplQueryObject query = new AplQueryObject(body);
        result.setResult(
            findTransactionService.getTransactionsByPeriod(query.getStartTime(), query.getEndTime(), query.getOrder().name())
        );
        return builder.bind(result).build();
    }

    public Response getOperationsCount(QueryObject body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        QueryCountResult result = new QueryCountResult();
        result.setQuery(body);
        AplQueryObject query = new AplQueryObject(body);
        result.setCount(
            findTransactionService.getTransactionsCountByPeriod(query.getStartTime(), query.getEndTime(), query.getOrder().name())
        );
        return builder.bind(result).build();
    }
}
