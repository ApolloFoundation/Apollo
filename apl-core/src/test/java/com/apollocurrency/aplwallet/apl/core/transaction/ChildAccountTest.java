/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.mock;

@EnableWeld
@ExtendWith(MockitoExtension.class)
public abstract class ChildAccountTest {

    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;

    @Mock
    TransactionValidator validator;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    HeightConfig heightConfig;
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    FeeCalculator calculator;
    @Mock
    ReferencedTransactionDao referencedTransactionDao;
    @Mock
    PhasingPollService phasingPollService;
    @Mock
    AccountControlPhasingService accountControlPhasingService;

    Blockchain blockchain = mock(Blockchain.class);
    AccountService accountService=mock(AccountService.class);
    AccountPublicKeyService accountPublicKeyService=mock(AccountPublicKeyService.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from()
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(accountService, AccountService.class, AccountServiceImpl.class))
        .addBeans(MockBean.of(accountPublicKeyService, AccountPublicKeyService.class, AccountPublicKeyServiceImpl.class))
        .build();

    final String accountRS = "APL-XR8C-K97J-QDZC-3YXHE";
    final String publicKey = "d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c";
    final String secretPhrase = "here we go again";
    final Account sender = new Account(Convert.parseAccountId(accountRS), 1000 * Constants.ONE_APL, 100 * Constants.ONE_APL, 0L, 0L, 0);
    final long senderId = sender.getId();

    static final String CHILD_SECRET_PHRASE_1 = "1234567890";
    static final byte[] CHILD_PUBLIC_KEY_1 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_1);
    static final long CHILD_ID_1 = AccountService.getId(CHILD_PUBLIC_KEY_1);

    static final String CHILD_SECRET_PHRASE_2 = "0987654321";
    static final byte[] CHILD_PUBLIC_KEY_2 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_2);
    static final long CHILD_ID_2 = AccountService.getId(CHILD_PUBLIC_KEY_2);

    static final Account child1 = new Account(CHILD_ID_1, 0L,0L, 0L, 0L, 0);
    static final Account child2 = new Account(CHILD_ID_2, 0L,0L, 0L, 0L, 0);

    ChildAccountAttachment attachment = new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2));

    TransactionCreator txCreator;
    TransactionApplier txApplier;
    TransactionValidator txValidator;
}