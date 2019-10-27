package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.*;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DexSmartContractServiceTest {
    @Mock private Web3j web3j;
    @Mock private KeyStoreService keyStoreService;
    @Mock
    private PropertiesHolder holder;
    @Mock private DexEthService dexEthService;
    @Mock private EthereumWalletService ethereumWalletService;
    @Mock
    private DexTransactionDao dexTransactionDao;
    private DexSmartContractService service;

    DexOrder order = new DexOrder(2L, 100L, "from-address", "to-address", OrderType.BUY, OrderStatus.OPEN, DexCurrencies.APL, 127_000_000L, DexCurrencies.ETH, BigDecimal.valueOf(0.0001), 500);

    @BeforeEach
    void setUp() {
        service = spy(new DexSmartContractService(web3j, holder, keyStoreService, dexEthService, ethereumWalletService, dexTransactionDao));
    }


    @Test
    void testHasFrozenMoneyForSellOrder() {
        order.setType(OrderType.SELL);

        boolean result = service.hasFrozenMoney(order);

        assertTrue(result);
    }
    @Test
    void testHasFrozenMoneyForBuyOrder() throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposits = List.of(new UserEthDepositInfo(order.getId(), BigDecimal.valueOf(0.000126), 2L), new UserEthDepositInfo(1L, BigDecimal.valueOf(0.000127), 1L), new UserEthDepositInfo(order.getId(), BigDecimal.valueOf(0.000127), 1L));
        doReturn(userDeposits).when(service).getUserFilledDeposits(order.getFromAddress());

        boolean result = service.hasFrozenMoney(order);

        assertTrue(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithoutUserDeposits() throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposits = List.of();
        doReturn(userDeposits).when(service).getUserFilledDeposits(order.getFromAddress());

        boolean result = service.hasFrozenMoney(order);

        assertFalse(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithException() throws AplException.ExecutiveProcessException {
        doThrow(new AplException.ExecutiveProcessException()).when(service).getUserFilledDeposits(order.getFromAddress());

        boolean result = service.hasFrozenMoney(order);

        assertFalse(result);
    }
}