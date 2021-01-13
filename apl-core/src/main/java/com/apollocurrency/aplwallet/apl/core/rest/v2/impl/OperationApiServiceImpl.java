package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.OperationApiService;
import com.apollocurrency.aplwallet.api.v2.model.QueryCountResult;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.api.v2.model.QueryResult;
import com.apollocurrency.aplwallet.apl.core.model.AplQueryObject;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FindTransactionService;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

@RequestScoped
@Slf4j
public class OperationApiServiceImpl implements OperationApiService {
    private final FindTransactionService findTransactionService;

    @Inject
    public OperationApiServiceImpl(FindTransactionService findTransactionService) {
        this.findTransactionService = Objects.requireNonNull(findTransactionService);
    }

    public Response getOperations(QueryObject body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        QueryResult result = new QueryResult();
        result.setQuery(Objects.requireNonNull(body));
        AplQueryObject query = new AplQueryObject(body);
        if (log.isTraceEnabled()) {
            log.trace("GetOperations query={}, from body startTimeUnix={} endTimeUnix={}", query, body.getStartTime(), body.getEndTime());
        }
        result.setResult(
            findTransactionService.getConfirmedTransactionsByQuery(query) //ByPeriod(query.getStartTime(), query.getEndTime(), query.getOrder().name())
        );
        result.setCount(result.getResult().size());
        return builder.bind(result).build();
    }

    public Response getOperationsCount(QueryObject body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        QueryCountResult result = new QueryCountResult();
        result.setQuery(Objects.requireNonNull(body));
        AplQueryObject query = new AplQueryObject(body);
        if (log.isTraceEnabled()) {
            log.trace("GetOperationsCount query={}, from body startTimeUnix={} endTimeUnix={}", query, body.getStartTime(), body.getEndTime());
        }
        result.setCount(
            findTransactionService.getConfirmedTransactionsCountByQuery(query)//ByPeriod(query.getStartTime(), query.getEndTime(), query.getOrder().name())
        );
        return builder.bind(result).build();
    }
}
