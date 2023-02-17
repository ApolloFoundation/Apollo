/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetTransactionsRequestTest {

    @Test
    void createRequest_NoIds() {
        Set<ConstraintViolation<GetTransactionsRequest>> violations = validate(new GetTransactionsRequest(Set.of(), UUID.randomUUID()));

        assertEquals(1, violations.size());
        assertEquals("size must be between 1 and 100", violations.stream().findFirst().get().getMessage());
    }

    @Test
    void createRequest_NullIdPresent() {
        HashSet<Long> ids = new HashSet<>();
        ids.add(null);
        ids.add(1L);

        Set<ConstraintViolation<GetTransactionsRequest>> violations = validate(new GetTransactionsRequest(ids, UUID.randomUUID()));

        assertEquals(1, violations.size());
        assertEquals("Null transaction ids are not allowed for GetTransactions request", violations.stream().findFirst().get().getMessage());
    }

    @Test
    void createRequest_NullTransactionIdsObject() {
        Set<ConstraintViolation<GetTransactionsRequest>> violations = validate(new GetTransactionsRequest(null, UUID.randomUUID()));

        assertEquals(1, violations.size());
        assertEquals("transactionIds object should not be null for GetTransactions request", violations.stream().findFirst().get().getMessage());
    }

    @Test
    void createRequest_NullChainId() {
        Set<ConstraintViolation<GetTransactionsRequest>> violations = validate(new GetTransactionsRequest(Set.of(1L), null));

        assertEquals(1, violations.size());
        assertEquals("must not be null", violations.stream().findFirst().get().getMessage());
    }

    @Test
    void createRequestOk() {
        GetTransactionsRequest request = new GetTransactionsRequest(Set.of(1L, -1L), UUID.randomUUID());

        Set<ConstraintViolation<GetTransactionsRequest>> constraintViolations = validate(request);

        assertEquals(0, constraintViolations.size());
        assertEquals(Set.of(1L, -1L), request.getTransactionIds());
        List<String> expectedStringIds = List.of("1", Long.toUnsignedString(-1));
        assertTrue(expectedStringIds.containsAll(request.getStringTransactionIds()), "expected string ids: " + expectedStringIds + " should contain all the actual string ids: " + request.getStringTransactionIds());
        request.setTransactionIds(List.of(Long.toUnsignedString(Long.MIN_VALUE), Long.toUnsignedString(-1)));
        assertEquals(Set.of(Long.MIN_VALUE, -1L), request.getTransactionIds());
    }

    private Set<ConstraintViolation<GetTransactionsRequest>> validate(GetTransactionsRequest request) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        return validator.validate(request);
    }
}