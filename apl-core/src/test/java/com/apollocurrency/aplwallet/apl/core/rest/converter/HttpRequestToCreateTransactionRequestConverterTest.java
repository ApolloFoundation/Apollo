/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.OrdinaryPaymentAttachment;
import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
@EnableWeld
class HttpRequestToCreateTransactionRequestConverterTest {
    HttpServletRequest request = mock(HttpServletRequest.class);


    private final ElGamalEncryptor elGamalEncryptor = mock(ElGamalEncryptor.class);
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(elGamalEncryptor, ElGamalEncryptor.class))
        .build();


    @BeforeEach
    void setUp() {
        doAnswer(e -> e.getArgument(0)).when(elGamalEncryptor).elGamalDecrypt(anyString());
        doReturn("secret").when(request).getParameter("secretPhrase");
    }

    @AfterEach
    void tearDown() {
        HttpParameterParserUtil.resetCDIComponents();
    }

    @Test
    void convert_defaultBroadcastAndValidate() throws ParameterException {
        CreateTransactionRequest convert = HttpRequestToCreateTransactionRequestConverter.convert(request, new Account(1L, 0), 0, 100L, new OrdinaryPaymentAttachment(), null, null, 10);

        assertFalse(convert.isValidate());
        assertFalse(convert.isBroadcast());
    }

    @Test
    void convert_falseBroadcastAndValidate() throws ParameterException {
        doReturn("true").when(request).getParameter("broadcast");
        doReturn("true").when(request).getParameter("validate");

        CreateTransactionRequest convert = HttpRequestToCreateTransactionRequestConverter.convert(request, new Account(1L, 0), 0, 100L, new OrdinaryPaymentAttachment(), false, false, 10);

        assertFalse(convert.isValidate());
        assertFalse(convert.isBroadcast());
    }

    @Test
    void convert_trueParamsBroadcastAndValidate_noParamsInRequest() throws ParameterException {
        CreateTransactionRequest convert = HttpRequestToCreateTransactionRequestConverter.convert(request, new Account(1L, 0), 0, 100L, new OrdinaryPaymentAttachment(), true, true, 10);

        assertTrue(convert.isValidate());
        assertTrue(convert.isBroadcast());
    }

    @Test
    void convert_trueParamsBroadcastAndValidate_falseParamsInRequest() throws ParameterException {
        doReturn("false").when(request).getParameter("broadcast");
        doReturn("false").when(request).getParameter("validate");

        CreateTransactionRequest convert = HttpRequestToCreateTransactionRequestConverter.convert(request, new Account(1L, 0), 0, 100L, new OrdinaryPaymentAttachment(), true, true, 10);

        assertFalse(convert.isValidate());
        assertFalse(convert.isBroadcast());
    }

    @Test
    void convert_trueParamsBroadcastAndValidate_trueParamsInRequest() throws ParameterException {
        doReturn("true").when(request).getParameter("broadcast");
        doReturn("true").when(request).getParameter("validate");

        CreateTransactionRequest convert = HttpRequestToCreateTransactionRequestConverter.convert(request, new Account(1L, 0), 0, 100L, new OrdinaryPaymentAttachment(), true, true, 10);

        assertTrue(convert.isValidate());
        assertTrue(convert.isBroadcast());
    }
}