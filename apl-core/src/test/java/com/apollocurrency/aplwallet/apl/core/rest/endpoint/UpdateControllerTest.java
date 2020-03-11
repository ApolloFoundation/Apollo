package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.ByteArrayConverterProvider;
import com.apollocurrency.aplwallet.apl.core.rest.PlatformSpecConverterProvider;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.exception.LegacyParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.PlatformSpec;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
//@EnableWeld
class UpdateControllerTest extends AbstractEndpointTest {
    TransactionCreator transactionCreator;
    @Mock
    HttpServletRequest req;
    @Mock
    TransactionValidator validator;
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        transactionCreator = new TransactionCreator(validator, new PropertiesHolder(), timeService, new FeeCalculator(), blockchain, processor);
        dispatcher.getProviderFactory()
            .register(ByteArrayConverterProvider.class)
            .register(LegacyParameterExceptionMapper.class)
            .register(PlatformSpecConverterProvider.class);
        UpdateController updateController = new UpdateController(restParametersParser, transactionCreator);
        dispatcher.getRegistry().addSingletonResource(updateController);
        dispatcher.getDefaultContextObjects().put(HttpServletRequest.class, req);
    }

    @Test
    void testSendUpdateSuccessful() throws URISyntaxException, UnsupportedEncodingException, ParameterException, AplException.ValidationException {
        when(elGamal.elGamalDecrypt(SECRET)).thenReturn(SECRET);
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * Constants.ONE_APL, 10000 * Constants.ONE_APL, 0, 0, CURRENT_HEIGHT);
        sender.setPublicKey(new PublicKey(sender.getId(), null, 0));
        doReturn(sender).when(accountService).getAccount(Convert.parseHexString(PUBLIC_KEY_SECRET));
        Transaction tx = mock(Transaction.class);
        UpdateV2Attachment attachment = new UpdateV2Attachment("https://test.com", Level.CRITICAL, new Version("1.23.4"), "https://con.com", BigInteger.ONE, Convert.parseHexString("111100ff"), Set.of(new PlatformSpec(Platform.WINDOWS, Architecture.AMD64), new PlatformSpec(Platform.ALL, Architecture.ARM)));
        doReturn(attachment).when(tx).getAttachment();
        doReturn(attachment.getTransactionType()).when(tx).getType();
        doAnswer(invocation -> {
            String argument = invocation.getArgument(0);
            switch (argument) {
                case "secretPhrase":
                    return SECRET;
            }
            return "";
        }).when(req).getParameter(anyString());
//        doReturn(tx).when(transactionCreator).createTransactionThrowingException(any(CreateTransactionRequest.class));

        MockHttpResponse response = sendPostRequest("/updates", "secretPhrase=" + SECRET + "&manifestUrl=https://test.com&level=CRITICAL&platformSpec=WINDOWS-AMD64,ALL-ARM" +
            "&version=1.23.4&cn=https://cn.com&serialNumber=1&signature=111100ff");
        System.out.println(response.getContentAsString());
    }
}