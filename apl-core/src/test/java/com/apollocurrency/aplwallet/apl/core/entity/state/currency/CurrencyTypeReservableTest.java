/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuanceAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncreaseAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CurrencyTypeReservableTest {
    public static final long CURRENCY_ID = 1L;
    @Mock
    Currency currency;
    @Mock
    Transaction tx;

    @Mock
    TransactionType txType;


    private final CurrencyType type = CurrencyType.RESERVABLE;

    @Test
    void code() {
        assertEquals(4, type.getCode());
    }

    @Test
    void validate_issuance_LatePhasing() {
        mockIssuanceTx(150, 150, 200, 2);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.validate(currency, tx, Set.of(type), 100L, false, 1200));

        assertEquals("Reservable currency activation height 200 not higher than transaction apply height 1200", ex.getMessage());
    }

    @Test
    void validate_issuance_ZeroReserve() {
        mockIssuanceTx(150, 150, 2000, 0);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.validate(currency, tx, Set.of(type), 100L, false, 1200));

        assertEquals("Minimum reserve per unit must be > 0", ex.getMessage());
    }

    @Test
    void validate_issuance_MinTotalReserveATMsOverflow() {
        mockIssuanceTx(Long.MAX_VALUE, Long.MAX_VALUE, 2000, 2);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.validate(currency, tx, Set.of(type), 100L, false, 1200));

        assertEquals("Result of multiplying x=2, y=9223372036854775807 exceeds the allowed range" +
            " [-9223372036854775808;9223372036854775807], transaction='null', type='MS_CURRENCY_ISSUANCE', sender='0'", ex.getMessage());
    }

    @Test
    void validate_issuance_MinTotalReserveATMsGreaterThanMaxAllowedATMsBalance() {
        mockIssuanceTx(200, 200, 2000, 2);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.validate(currency, tx, Set.of(type), 300L, false, 1200));

        assertEquals("Total minimum reserve is too large", ex.getMessage());
    }

    @Test
    void validate_issuance_ReserveSupplyIsNotGreaterThanInitialSupply() {
        mockIssuanceTx(100, 200, 2000, 2);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.validate(currency, tx, Set.of(type), 300L, false, 1200));

        assertEquals("Reserve supply must exceed initial supply", ex.getMessage());
    }

    @Test
    void validate_issuance_NotMintableReserveSupplyLessThanMaxSupply() {
        mockIssuanceTx(190, 200, 2000, 2);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.validate(currency, tx, Set.of(type), 500L, false, 1200));

        assertEquals("Max supply must not exceed reserve supply for reservable and non-mintable currency", ex.getMessage());
    }

    @Test
    void validate_issuance_OKNotMintable() throws AplException.ValidationException {
        mockIssuanceTx(200, 200, 2000, 2);

        type.validate(currency, tx, Set.of(type), 500L, false, 1200);
    }

    @Test
    void validate_issuance_OKMintable() throws AplException.ValidationException {
        mockIssuanceTx(190, 200, 2000, 2);

        type.validate(currency, tx, Set.of(type, CurrencyType.MINTABLE), 500L, false, 1200);
    }

    @Test
    void validate_reserveIncrease_OKNoCurrency() throws AplException.ValidationException {
        mockReserveIncreaseTx(2);

        type.validate(null, tx, Set.of(type, CurrencyType.MINTABLE), 500L, false, 1200);

    }

    @Test
    void validate_reserveIncrease_ActiveCurrency() {
        mockReserveIncreaseTx(2);
        doReturn(1300).when(currency).getIssuanceHeight();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () ->type.validate(currency, tx, Set.of(type, CurrencyType.MINTABLE), 500L, false, 1301));

        assertEquals("Cannot increase reserve for active currency", ex.getMessage());
    }

    @Test
    void validate_reserveIncrease_OverflowTotalATMsAmountToReserve() {
        mockReserveIncreaseTx(100);
        doReturn(1300).when(currency).getIssuanceHeight();
        doReturn(Long.MAX_VALUE).when(currency).getReserveSupply();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () ->type.validate(currency, tx, Set.of(type), 500L, false, 1200));

        assertEquals("Result of multiplying x=9223372036854775807, y=100 exceeds the allowed range " +
            "[-9223372036854775808;9223372036854775807], transaction='null', type='MS_RESERVE_INCREASE', sender='0'", ex.getMessage());
    }


    @Test
    void validate_reserveIncrease_OK() throws AplException.ValidationException {
        mockReserveIncreaseTx(100);
        doReturn(1300).when(currency).getIssuanceHeight();
        doReturn(200L).when(currency).getReserveSupply();

        type.validate(currency, tx, Set.of(type), 500L, false, 1200);
    }

    @Test
    void validate_otherTxType() throws AplException.ValidationException {
        doReturn(txType).when(tx).getType();
        doReturn(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT).when(txType).getSpec();

        type.validate(currency, tx, Set.of(type), 10_000, true, 1000);
    }

    private void mockIssuanceTx(long reserveSupply, long maxSupply, int issuanceHeight, long minReservePerUnitATM) {
        MonetarySystemCurrencyIssuanceAttachment attachment = new MonetarySystemCurrencyIssuanceAttachment("TEST CURRENCY", "TST", "Just test coin", (byte) 4, 100, reserveSupply, maxSupply, issuanceHeight, minReservePerUnitATM, 1, 100, (byte) 1, (byte) 0, (byte) 0);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(txType).when(tx).getType();
        doReturn(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE).when(txType).getSpec();
    }

    private void mockReserveIncreaseTx(long amountPerUnitATM) {
        MonetarySystemReserveIncreaseAttachment attachment = new MonetarySystemReserveIncreaseAttachment(CURRENCY_ID, amountPerUnitATM);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(txType).when(tx).getType();
        doReturn(TransactionTypes.TransactionTypeSpec.MS_RESERVE_INCREASE).when(txType).getSpec();
    }

}