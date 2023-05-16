/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MockUtils {
    public static Stubber doAnswerWithSuppliers(Map<Integer, Supplier<?>> returnedObjs) {
        return Mockito.doAnswer(new Answer<>() {
            int counter = 0;
            @Override
            public Object answer(InvocationOnMock invocation) {
                return returnedObjs.get(++counter).get();
            }
        });
    }

    public static Stubber doAnswer(Map<Integer, ?> returnedObjs) {
        Map<Integer, Supplier<?>> valueSuppliers = returnedObjs.keySet().stream().collect(Collectors.toMap(Function.identity(), (k) -> () -> returnedObjs.get(k)));
        return doAnswerWithSuppliers(valueSuppliers);
    }

}
