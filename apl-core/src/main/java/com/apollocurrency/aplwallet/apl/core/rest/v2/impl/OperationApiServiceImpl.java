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

        return Response.ok().build();
    }


    private boolean validateQuery(QueryObject query) {
        boolean rc = false;

        query.getAccounts();
        query.getStartTime();
        query.getEndTime(); //timestamp
        query.getFirst();
        query.getLast(); //height
        query.getTransactionStatus();

        query.getPage();
        query.getPerPage();
        query.getOrderBy();


        return rc;
    }
}
