package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.OperationApiService;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
public class OperationApiServiceImpl implements OperationApiService {
    public Response getOperations(QueryObject body, SecurityContext securityContext) throws NotFoundException {

        return Response.ok().build();
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


    private boolean validateQuery(QueryObject query) {
        boolean rc = false;

        query.getAccounts();
        query.getStartTime();
        query.getEndTime(); //timestamp
        query.getFirst();
        query.getLast(); //height

        query.getPage();
        query.getPerPage();
        query.getOrderBy();


        return rc;
    }
}
