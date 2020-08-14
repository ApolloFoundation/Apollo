/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.OperationApiService;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FindTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@ExtendWith(MockitoExtension.class)
class OperationApiServiceImplTest {

    @Mock
    SecurityContext securityContext;
    @Mock
    FindTransactionService findTransactionService;

    QueryObject queryObject = new QueryObject();
    OperationApiService operationApiService;

    @BeforeEach
    void setUp() {
        operationApiService = new OperationApiServiceImpl(findTransactionService);
    }

    @Test
    void getOperationsWithNullQuery() {
        assertThrows(RuntimeException.class, () -> operationApiService.getOperations(null, securityContext));
    }

    @Test
    void getOperations() {
        Response response = operationApiService.getOperations(queryObject, securityContext);
        assertNotNull(response);
    }

    @Test
    void getOperationsCountWithNUllQuery() {
        assertThrows(RuntimeException.class, () -> operationApiService.getOperationsCount(null, securityContext));
    }

    @Test
    void getOperationsCount() {
        Response response = operationApiService.getOperationsCount(queryObject, securityContext);
        assertNotNull(response);
    }
}